package com.vd.diff.split.virtual;

import java.util.ArrayList;
import java.util.List;

public class VirtualSplitsBuilder {
    private final List<VirtualSplit> splits = new ArrayList<>();

    public VirtualSplitsBuilder add(String startKey, String stopKey) {
        splits.add(new VirtualSplit(startKey, stopKey));
        return this;
    }

    public VirtualSplits build() {
        return new VirtualSplits(splits);
    }
}
