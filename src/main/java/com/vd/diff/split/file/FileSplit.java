package com.vd.diff.split.file;

import com.google.common.base.Preconditions;
import com.vd.diff.split.virtual.VirtualSplit;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class FileSplit extends VirtualSplit {

    private final long startKeyOffset;
    private final long stopKeyOffset;

    public FileSplit(String startKey, String stopKey, long startKeyOffset, long stopKeyOffset) {
        super(startKey, stopKey);

        Preconditions.checkArgument(startKeyOffset >= 0 && startKeyOffset <= stopKeyOffset);

        this.startKeyOffset = startKeyOffset;
        this.stopKeyOffset = stopKeyOffset;
    }
}
