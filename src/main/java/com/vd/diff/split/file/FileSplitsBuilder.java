package com.vd.diff.split.file;

import java.util.ArrayList;
import java.util.List;

public class FileSplitsBuilder {
    private final List<FileSplit> splits = new ArrayList<>();

    public FileSplitsBuilder add(
            String startKey,
            String stopKey,
            long startKeyOffset,
            long stopKeyOffset) {

        splits.add(new FileSplit(startKey, stopKey, startKeyOffset, stopKeyOffset));
        return this;
    }

    public FileSplits build() {
        return new FileSplits(splits);
    }
}
