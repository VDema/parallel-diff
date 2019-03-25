package com.vd.diff.file;

import com.google.common.base.Preconditions;
import com.vd.diff.content.Row;
import com.vd.diff.content.parser.RowParser;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BufferedRowReader {
    private static final String[] EMPTY_STRING_ARRAY = {};

    private final FastAccessRowFile delegate;

    public BufferedRowReader(FastAccessRowFile delegate) {
        this.delegate = delegate;
    }

    private byte[] byteBuffer = new byte[4096];
    private byte[] tmpByteBuffer = new byte[4096];

    private LinkedList<String> bufferedLines = new LinkedList<>();

    public Row nextRow() throws IOException {
        if (bufferedLines.isEmpty()) {
            fillBuffer();
        }

        if (bufferedLines.isEmpty()) {
            return null;
        }

        return RowParser.toRow(bufferedLines.pollFirst());
    }

    private void fillBuffer() throws IOException {
        long cur = delegate.pos();

        String[] lines = readLines();
        if (lines.length == 0) {
            return;
        }

        bufferedLines.addAll(Arrays.asList(lines));

        if (bufferedLines.isEmpty()) {
            return;
        }

        String lastLine = bufferedLines.getLast();

        boolean readExact = false;
        if ('\n' == lastLine.charAt(lastLine.length() - 1)) {
            readExact = true;
        }

        if (!readExact) {
            String uncompletedLine = bufferedLines.pollLast();

            int lastLineOffset = 0;
            for (String bufferedLine : bufferedLines) {
                lastLineOffset += bufferedLine.length() + 1;
            }

            delegate.seek(cur + lastLineOffset);

            //noinspection ConstantConditions
            delegate.readNext(tmpByteBuffer, uncompletedLine.length());
            String line = new String(tmpByteBuffer, 0, uncompletedLine.length());

            Preconditions.checkState(line.equals(uncompletedLine));

            delegate.seek(cur + lastLineOffset);
        }
    }

    private String[] readLines() throws IOException {
        int readBytesAmount = delegate.readNext(byteBuffer);
        if (readBytesAmount <= 0) {
            return EMPTY_STRING_ARRAY;
        }

        String rawLines = new String(byteBuffer, 0, readBytesAmount);
        String lastLine = rawLines;

        while (!lastLine.contains("\n")) {
            readBytesAmount = delegate.readNext(byteBuffer);
            if (readBytesAmount <= 0) {
                log.warn("Last line in the file is uncompleted with `\\\n`. " +
                        "So this line can't be compared. Line: [{}]", rawLines);
                return EMPTY_STRING_ARRAY;
            }

            lastLine = new String(byteBuffer, 0, readBytesAmount);
            rawLines += lastLine;
        }

        return rawLines.split("\n");
    }
}
