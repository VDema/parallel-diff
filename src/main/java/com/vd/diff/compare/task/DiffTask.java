package com.vd.diff.compare.task;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.vd.diff.compare.report.DiffMetrics;
import com.vd.diff.compare.report.DiffReport;
import com.vd.diff.content.Row;
import com.vd.diff.file.FastAccessRowFile;
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
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

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

            reportBuilder
                    .timeElapsed(stopwatch.elapsed(TimeUnit.MILLISECONDS));

            return new DiffResult(file, reportBuilder.build());
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

        FileSplit leftSplit = intersection.tryLeft().get();
        FileSplit rightSplit = intersection.tryRight().get();

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

        DiffMetrics.MetricsAggregator metricsAggregator = new DiffMetrics.MetricsAggregator();
        BufferedRowReader leftReader = new BufferedRowReader(leftRowFile);
        BufferedRowReader rightReader = new BufferedRowReader(rightRowFile);

        while (leftRow != null && rightRow != null) {
            if (leftRow.isKeyLower(rightRow)) {
                writeLine(writer, leftRow.getRaw());
                metricsAggregator.newLeft();

                leftRow = nextRow(leftReader);
            } else if (leftRow.isKeyGreater(rightRow)) {
                writeLine(writer, rightRow.getRaw());
                metricsAggregator.newRight();

                rightRow = nextRow(rightReader);
            } else {
                writeLine(writer, leftRow.diffEvents(rightRow));
                metricsAggregator.differentEvents();

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
                .metrics(metricsAggregator.getMetrics());
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

        FastAccessRowFile.OffsetRow curRow = new FastAccessRowFile.OffsetRow(split.getStartKeyOffset(), row);
        Preconditions.checkState(curRow.getRowKey().equals(split.getStartKey()));

        long startPos = split.getStartKeyOffset();
        long endPos = split.getStopKeyOffset();

        FastAccessRowFile.OffsetRow closestRow = curRow;

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

    //ToDo: func should use buffered read instead of read byte by byte
    //ToDo: this implementation is completely inefficient
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

    private class BufferedRowReader {
        private final FastAccessRowFile delegate;

        BufferedRowReader(FastAccessRowFile delegate) {
            this.delegate = delegate;
        }

        private byte[] byteBuffer = new byte[1024];

        private String buffer;

        private Row nextRow() throws IOException {
            return delegate.nextRow();
        }

        private Row nextBufferedRow() throws IOException {
            if (StringUtils.isBlank(buffer)) {
                int readAmount = delegate.readNext(byteBuffer);
                if (readAmount <= 0) {
                    return null;
                }
            }

            int nextLinePos = buffer.indexOf("\n");

            throw new NotImplementedException("This functionality still not implemented");
        }
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
