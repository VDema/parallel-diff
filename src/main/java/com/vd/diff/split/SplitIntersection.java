package com.vd.diff.split;

import com.vd.diff.split.file.FileSplit;
import com.vd.diff.split.virtual.VirtualSplit;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class SplitIntersection {
    private final VirtualSplit baseSplit;
    private final FileSplit leftSplit;
    private final FileSplit rightSplit;

    public Optional<FileSplit> tryLeft() {
        return Optional.ofNullable(leftSplit);
    }

    public Optional<FileSplit> tryRight() {
        return Optional.ofNullable(rightSplit);
    }

    public String getStartKey() {
        return baseSplit.getStartKey();
    }

    public String getStopKey() {
        return baseSplit.getStopKey();
    }

    public void validate() {
        validateSplit(leftSplit);
        validateSplit(rightSplit);
    }

    private void validateSplit(FileSplit split) {
        if (split != null) {
            if (split.getStartKey().compareTo(baseSplit.getStopKey()) > 0
                    || split.getStopKey().compareTo(baseSplit.getStartKey()) < 0) {
                throw new IllegalStateException(
                        String.format("Invalid split: [%s]. Should be within base Split: [%s]", split, baseSplit));
            }
        }
    }
}
