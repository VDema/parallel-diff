package com.vd.diff.file;

import com.vd.diff.content.Row;
import com.vd.diff.content.parser.RowBuilder;
import com.vd.diff.utils.FileUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

public class FastAccessRowFile implements AutoCloseable {

    private final File file;
    private final FastAccessFile delegate;
    private final long fileSize;
    private final Row firstRow;
    private final Row lastRow;
    private final int longestLineInBytes;

    private static final int DEFAULT_LONGEST_LINE_IN_BYTES = 4096;

    public FastAccessRowFile(File file) throws IOException {
        this(file, DEFAULT_LONGEST_LINE_IN_BYTES);
    }

    public FastAccessRowFile(File file, int longestLineInBytes) throws IOException {
        this.longestLineInBytes = longestLineInBytes <= 0 ? DEFAULT_LONGEST_LINE_IN_BYTES : longestLineInBytes;

        this.file = file;
        this.delegate = createAccessFileUnchecked(file);
        this.fileSize = delegate.length();
        this.firstRow = extractFirstRow();
        this.lastRow = extractLastRow();
    }

    private FastAccessRowFile(
            File file,
            FastAccessFile delegate,
            long fileSize,
            Row firstRow,
            Row lastRow,
            int longestLineInBytes) {

        this.file = file;
        this.delegate = delegate;
        this.fileSize = fileSize;
        this.firstRow = firstRow;
        this.lastRow = lastRow;
        this.longestLineInBytes = longestLineInBytes;
    }

    public FastAccessRowFile copyForThreadSafeUsage() {
        return new FastAccessRowFile(
                file,
                createAccessFileUnchecked(file),
                fileSize,
                firstRow,
                lastRow,
                longestLineInBytes
        );
    }

    private FastAccessFile createAccessFileUnchecked(File file) {
        try {
            return new FastAccessFile(file, "r");
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Something is wrong. Unable to create RandomAccessFile for reading. File: "
                    + file.getAbsolutePath());
        }
    }

    public long getFileSize() {
        return fileSize;
    }

    public Row getFirstRow() {
        return firstRow;
    }

    public void seek(long pos) throws IOException {
        if (pos > fileSize) {
            pos = fileSize;
        }
        delegate.seek(pos);
    }

    public int readNext(byte[] bytes) throws IOException {
        return delegate.read(bytes, 0, bytes.length);
    }

    public Row nextRow() throws IOException {
        return Optional.ofNullable(delegate.readLine())
                .map(RowBuilder::toRow)
                .orElse(null);
    }

    public OffsetRow nextRow(long splitOffset) throws IOException {
        seek(splitOffset);

        int c = delegate.read();
        if (c == '\n') {
            c = delegate.read();
            if (c == '\r') {
                long cur = delegate.getFilePointer();
                if (delegate.read() != '\n') {
                    seek(cur);
                }
                long lastRowOffset = fileSize - lastRow.getKey().length() * 2 - 1 - 1;
                return new OffsetRow(lastRowOffset, lastRow);
            }
            String rawRow = new String(Character.toChars(c)) + delegate.readLine();

            long offset = splitOffset + 1;
            checkState(offset, rawRow);
            return new OffsetRow(offset, RowBuilder.toRow(rawRow));
        }

        delegate.readLine();
        long lineOffset = delegate.getFilePointer();
        String nextLine = delegate.readLine();

        checkState(lineOffset, nextLine);
        return new OffsetRow(lineOffset, RowBuilder.toRow(nextLine));
    }

    private void checkState(long lineOffset, String expectedLine) throws IOException {
        seek(lineOffset);
        String actualLine = delegate.readLine();
        if (!actualLine.equals(expectedLine)) {
            throw new IllegalArgumentException("Incorrect calculations.");
        }
    }

    @AllArgsConstructor
    @Getter
    public static class OffsetRow {
        private final long offset;
        private final Row row;

        public String getRowKey() {
            return row.getKey();
        }
    }

    @Override
    public void close() {
        FileUtils.closeSilently(delegate);
    }

    public String getFirstRowKey() {
        return firstRow.getKey();
    }

    public Row getLastRow() {
        return lastRow;
    }

    public String getLastRowKey() {
        return lastRow.getKey();
    }

    public int getLongestLineInBytes() {
        return longestLineInBytes;
    }

    private Row extractFirstRow() {
        return RowBuilder.toRow(extractRawFirstLine());
    }

    private String extractRawFirstLine() {
        try {
            delegate.seek(0);
            return delegate.readLine();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Row extractLastRow() throws IOException {
        return RowBuilder.toRow(extractRawLastLine(longestLineInBytes));
    }

    private String extractRawLastLine(int shiftByteSize) throws IOException {
        if (shiftByteSize >= fileSize) {
            throw new IllegalArgumentException("Shift byte size if too big");
        }

        long posFromTail = fileSize - shiftByteSize;

        delegate.seek(posFromTail);

        String prevLine = null;
        String lastLine = null;
        String curLine;

        while ((curLine = delegate.readLine()) != null) {
            prevLine = lastLine;
            lastLine = curLine;
        }

        if (prevLine == null) {
            //Record size is more then batch size, so needs to increase batch size.
            //StackOverflowError is possible. Should Be TailRecursion
            return extractRawLastLine(shiftByteSize * 2);
        }

        return lastLine;
    }


}
