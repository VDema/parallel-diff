package com.vd.diff.split.file;

import com.vd.diff.split.AbstractSplits;
import com.vd.diff.split.virtual.VirtualSplit;
import java.util.List;
import java.util.stream.Collectors;

public class FileSplits extends AbstractSplits<FileSplit> {

    public FileSplits(List<FileSplit> splits) {
        super(splits);
    }

    public List<FileSplit> getIntersected(VirtualSplit otherSplit) {
        return splits.stream()
                .filter(split -> split.isIntersects(otherSplit))
                .collect(Collectors.toList());
    }

    public static FileSplitsBuilder builder() {
        return new FileSplitsBuilder();
    }
}
