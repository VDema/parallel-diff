package com.vd.diff.split.file;

import com.vd.diff.split.virtual.VirtualFilePosition;
import lombok.Getter;

@Getter
public class FilePosition extends VirtualFilePosition {

    private final long keyOffset;

    public FilePosition(String key, long keyOffset) {
        super(key);
        this.keyOffset = keyOffset;
    }
}
