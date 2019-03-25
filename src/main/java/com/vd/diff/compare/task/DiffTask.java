package com.vd.diff.compare.task;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.vd.diff.compare.report.DiffMetrics;
import com.vd.diff.compare.report.DiffReport;
import com.vd.diff.content.Row;
import com.vd.diff.file.BufferedRowReader;
import com.vd.diff.file.FastAccessRowFile;
import com.vd.diff.file.OffsetRow;
import com.vd.diff.split.SplitIntersection;
import com.vd.diff.split.file.FileSplit;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Builder
@Slf4j
public class DiffTask implements Callable<DiffTask.DiffResult> {

    private FastAccessRowFile leftRowFile;
    private FastAccessRowFile rightRowFile;

    private final SplitIntersection intersection;
    private final int intersectionFileOrderNum;

    private final File diffDir;

    public boolean isNotEmpty() {
        return intersection.tryLeft().isPresent() || intersection.tryRight().isPresent();
    }

    @Override
    public DiffResult call() throws Exception {
        File file = new File(createDiffFileName());

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            Stopwatch stopwatch = Stopwatch.createStarted();
            DiffReport.DiffReportBuilder reportBuilder = diffSplits(writer);
            stopwatch.stop();

            DiffReport report = reportBuilder
                    .timeElapsed(stopwatch.elapsed(TimeUnit.MILLISECONDS))
                    .build();

            return new DiffResult(file, report);
        }
    }

    private DiffReport.DiffReportBuilder diffSplits(BufferedWriter writer) throws IOException {
        if (!intersection.tryLeft().isPresent() || !intersection.tryRight().isPresent()) {
            FileSplit fileSplit = intersection.tryLeft().isPresent()
                    ? intersection.tryLeft().get()
                    : intersection.tryRight().get();

            FastAccessRowFile accessFile = intersection.tryLeft().isPresent()
                    ? leftRowFile
                    : rightRowFile;

            accessFile.seek(fileSplit.getStartKeyOffset());
            int skippedRows = writeRecordsWithinAndReturnSkipped(accessFile, writer);

            return DiffReport.builder()
                    .leftSkippedRows(intersection.tryLeft().isPresent() ? skippedRows : 0)
                    .rightSkippedRows(intersection.tryRight().isPresent() ? skippedRows : 0);
        }

        return diffSplits(writer, intersection.tryLeft().get(), intersection.tryRight().get());
    }

    private DiffReport.DiffReportBuilder diffSplits(
            BufferedWriter writer,
            FileSplit leftSplit,
            FileSplit rightSplit) throws IOException {

        leftRowFile.seek(leftSplit.getStartKeyOffset());
        rightRowFile.seek(rightSplit.getStartKeyOffset());

        SearchRowResult leftSearchRes = findFirstWithin(leftRowFile, leftSplit);
        SearchRowResult rightSearchRes = findFirstWithin(rightRowFile, rightSplit);

        int leftSkippedRows = leftSearchRes.rowsSkipped;
        int rightSkippedRows = rightSearchRes.rowsSkipped;

        if (leftSearchRes.empty() || rightSearchRes.empty()) {
            log.warn(String.format("Splits configured as intersected but there is no found any intersected record." +
                            "Skipped records from the left: [%d]. Skipped from the right: [%d]",
                    leftSkippedRows, rightSkippedRows));
        }

        Row leftRow = leftSearchRes.row;
        Row rightRow = rightSearchRes.row;

        DiffMetrics.MetricsAggregator metrics = new DiffMetrics.MetricsAggregator();
        BufferedRowReader leftReader = new BufferedRowReader(leftRowFile);
        BufferedRowReader rightReader = new BufferedRowReader(rightRowFile);

        while (leftRow != null && rightRow != null) {
            if (leftRow.isKeyLower(rightRow)) {
                writeLine(writer, leftRow.getRaw());
                metrics.newLeft();

                leftRow = nextRow(leftReader);
            } else if (leftRow.isKeyGreater(rightRow)) {
                writeLine(writer, rightRow.getRaw());
                metrics.newRight();

                rightRow = nextRow(rightReader);
            } else {
                if (!leftRow.getRaw().equals(rightRow.getRaw())) {
                    writeLine(writer, leftRow.diffEvents(rightRow));
                    metrics.differentEvents();
                } else {
                    metrics.sameLines();
                }

                leftRow = nextRow(leftReader);
                rightRow = nextRow(rightReader);
            }
        }

        //There could be left some values which is new and should be taken
        Row row = leftRow != null ? leftRow : rightRow;
        BufferedRowReader rowReader = leftRow != null ? leftReader : rightReader;

        while (row != null) {
            writeLine(writer, row.getRaw());
            row = nextRow(rowReader);
        }

        return DiffReport.builder()
                .leftSkippedRows(leftSkippedRows)
                .rightSkippedRows(rightSkippedRows)
                .metrics(metrics.getMetrics());
    }

    private SearchRowResult findFirstWithin(FastAccessRowFile rowFile, FileSplit split) throws IOException {
        //BinarySearch
        seekToTheClosest(rowFile, split);

        //LinearSearch
        return findFirstWithingIntersection(rowFile);
    }

    /*
     * Here it's expected to apply binary search algorithm to find closest row from the beginning of the split
     * */
    private void seekToTheClosest(FastAccessRowFile rowFile, FileSplit split) throws IOException {
        rowFile.seek(split.getStartKeyOffset());
        Row row = rowFile.nextRow();

        OffsetRow curRow = new OffsetRow(split.getStartKeyOffset(), row);
        Preconditions.checkState(curRow.getRowKey().equals(split.getStartKey()));

        long startPos = split.getStartKeyOffset();
        long endPos = split.getStopKeyOffset();

        OffsetRow closestRow = curRow;

        while (endPos - startPos > rowFile.getLongestLineInBytes()) {
            long mid = startPos + (endPos - startPos) / 2;

            curRow = rowFile.nextRow(mid);

            if (curRow.getRowKey().compareTo(intersection.getStartKey()) > 0) {
                endPos = mid;
            } else {
                startPos = mid;
                closestRow = curRow;
            }
        }

        rowFile.seek(closestRow.getOffset());
    }

    private SearchRowResult findFirstWithingIntersection(FastAccessRowFile rowFile) throws IOException {
        Row row = null;
        Row prevRow;
        Row curRow;

        int rowsSkipped = 0;

        while ((curRow = rowFile.nextRow()) != null) {
            prevRow = row;
            row = curRow;

            if (prevRow != null) {
                //Here there was an issue that there was no found any row at start of the process.
                if (row.isKeyGreaterOrEqual(intersection.getStopKey())
                        && prevRow.isKeyLower(intersection.getStartKey())
                        && prevRow.isKeyLower(intersection.getStopKey())) {

                    log.warn(String.format(
                            "Intersection range is too small. Should be validated better before running this task. " +
                                    "Prev row: [%s]. CurrentRow: [%s]. Intersection start: [%s]. Stop: [%s]",
                            prevRow, row, intersection.getStartKey(), intersection.getStopKey()));

                    return new SearchRowResult(null, rowsSkipped);
                }
            }

            if (row.isKeyGreaterOrEqual(intersection.getStopKey())) {
                return new SearchRowResult(null, rowsSkipped);
            }

            if (row.isKeyGreaterOrEqual(intersection.getStartKey())) {
                return new SearchRowResult(row, rowsSkipped);
            }

            rowsSkipped++;
        }

        return new SearchRowResult(null, rowsSkipped);
    }

    private void writeLine(BufferedWriter writer, String val) throws IOException {
        writer.write(val + "\n");
    }

    private Row nextRow(BufferedRowReader reader) throws IOException {
        Row row;

        while ((row = reader.nextRow()) != null) {

            if (row.isKeyGreaterOrEqual(intersection.getStopKey())) {
                return null;
            }

            if (row.isKeyGreaterOrEqual(intersection.getStartKey())) {
                return row;
            }
        }

        return null;
    }

    private int writeRecordsWithinAndReturnSkipped(FastAccessRowFile rowFile, BufferedWriter diffChannel) throws IOException {
        Row record;
        int skippedRecords = 0;
        while ((record = rowFile.nextRow()) != null) {
            if (record.isKeyGreaterOrEqual(intersection.getStopKey())) {
                break;
            }

            if (record.isKeyGreaterOrEqual(intersection.getStartKey())) {
                writeLine(diffChannel, record.getRaw());
            }

            skippedRecords++;
        }

        return skippedRecords;
    }

    private String createDiffFileName() {
        return diffDir.getName() + "/diff_" + intersectionFileOrderNum + ".txt";
    }

    @AllArgsConstructor
    @ToString
    private static class SearchRowResult {
        private final Row row;
        private final int rowsSkipped;

        boolean exists() {
            return row != null;
        }

        boolean empty() {
            return !exists();
        }
    }

    @Getter
    @AllArgsConstructor
    public class DiffResult {
        private final File diffFile;
        private final DiffReport diffReport;

        public int getDiffOrderNum() {
            return intersectionFileOrderNum;
        }
    }
}
