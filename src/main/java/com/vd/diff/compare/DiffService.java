package com.vd.diff.compare;

import com.google.common.base.Preconditions;
import com.vd.diff.compare.report.DiffReport;
import com.vd.diff.compare.task.DiffTask;
import com.vd.diff.compare.task.DiffThreadFactory;
import com.vd.diff.file.FastAccessRowFile;
import com.vd.diff.split.SplitIntersection;
import com.vd.diff.split.SplitService;
import com.vd.diff.split.file.FileSplits;
import com.vd.diff.utils.FileUtils;
import com.vd.diff.utils.KeyUtils;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DiffService {

    private static final int AVAILABLE_PROCESSORS = Runtime.getRuntime().availableProcessors();
    private static final int DEFAULT_SPLITS_NUM = AVAILABLE_PROCESSORS * 3;
    private static final int TAKEN_PROCESSORS = AVAILABLE_PROCESSORS - 1;

    private DiffMerger diffMerger = new DiffMerger();

    public File diffFiles(File fileA, File fileB, String outFilePath) throws IOException {
        try (FastAccessRowFile accessFileA = new FastAccessRowFile(fileA);
             FastAccessRowFile accessFileB = new FastAccessRowFile(fileB)) {

            FileSplits splitsA = SplitService.split(accessFileA, DEFAULT_SPLITS_NUM);
            FileSplits splitsB = SplitService.split(accessFileB, DEFAULT_SPLITS_NUM);

            String commonStartKey = KeyUtils.lowerKey(accessFileA.getFirstRowKey(), accessFileB.getFirstRowKey());
            String commonStopKey = KeyUtils.higherKey(accessFileA.getLastRowKey(), accessFileB.getLastRowKey());

            List<SplitIntersection> intersections = SplitService
                    .splitRange(commonStartKey, commonStopKey, DEFAULT_SPLITS_NUM)
                    .intersections(splitsA, splitsB);

            intersections.forEach(SplitIntersection::validate);

            File diffDir = createDirForDiffs();

            List<DiffTask> diffTasks = IntStream.range(0, intersections.size())
                    .mapToObj(idx -> DiffTask.builder()
                            .leftRowFile(accessFileA.copyForThreadSafeUsage())
                            .rightRowFile(accessFileB.copyForThreadSafeUsage())
                            .intersection(intersections.get(idx))
                            .intersectionFileOrderNum(idx)
                            .diffDir(diffDir)
                            .build())
                    .filter(DiffTask::isNotEmpty)
                    .collect(Collectors.toList());

            //One core will be used to maintain Executors
            List<DiffTask.DiffResult> diffResults = executeDiffTasks(diffTasks);

            logTheDiffProcessReport(diffResults);

            return diffMerger.merge(diffResults, outFilePath);
        }
    }

    private static void logTheDiffProcessReport(List<DiffTask.DiffResult> diffResults) {
        DiffReport diffReport = diffResults.stream()
                .map(DiffTask.DiffResult::getDiffReport)
                .peek(report -> log.info(report.toString()))
                .reduce(DiffReport.builder().build(), DiffReport::merge);

        log.info("Total diff result: {}. Within Threads: {}. Approximate diff time: {}",
                diffReport, TAKEN_PROCESSORS, diffReport.getTimeElapsed() / TAKEN_PROCESSORS);
    }

    private static List<DiffTask.DiffResult> executeDiffTasks(List<DiffTask> diffTasks) {
        ExecutorService executorService = Executors
                .newFixedThreadPool(TAKEN_PROCESSORS, DiffThreadFactory.create("parallel-diff"));

        List<Future<DiffTask.DiffResult>> diffTaskFutures = diffTasks.stream()
                .map(executorService::submit)
                .collect(Collectors.toList());

        List<DiffTask.DiffResult> diffResults = diffTaskFutures.stream()
                .map(DiffService::getDiffResultUnchecked)
                .sorted(Comparator.comparing(DiffTask.DiffResult::getDiffOrderNum))
                .collect(Collectors.toList());

        executorService.shutdown();

        return diffResults;
    }

    private static DiffTask.DiffResult getDiffResultUnchecked(Future<DiffTask.DiffResult> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException ex) {
            throw new IllegalStateException("Some of diffEvents tasks failed.", ex);
        }
    }

    private static File createDirForDiffs() throws IOException {
        File diffDir = new File("diff");
        FileUtils.deleteDirectory(diffDir);

        Preconditions.checkState(diffDir.mkdir());
        return diffDir;
    }
}
