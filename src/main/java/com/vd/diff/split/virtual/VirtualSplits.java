package com.vd.diff.split.virtual;

import com.vd.diff.split.AbstractSplits;
import com.vd.diff.split.SplitIntersection;
import com.vd.diff.split.file.FileSplit;
import com.vd.diff.split.file.FileSplits;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class VirtualSplits extends AbstractSplits<VirtualSplit> {

    public VirtualSplits(List<VirtualSplit> splits) {
        super(splits);
    }

    public int getSize() {
        return splits.size();
    }

    public List<VirtualSplit> getSplits() {
        return new ArrayList<>(splits);
    }

    public List<SplitIntersection> intersections(FileSplits leftSplits, FileSplits rightSplits) {
        return splits.stream()
                .map(split -> {
                    List<FileSplit> leftIntersected = leftSplits.getIntersected(split);
                    List<FileSplit> rightIntersected = rightSplits.getIntersected(split);

                    return new SplitIntersection(
                            split,
                            tryJoin(leftIntersected).orElse(null),
                            tryJoin(rightIntersected).orElse(null)
                    );
                }).collect(Collectors.toList());
    }

    private Optional<FileSplit> tryJoin(List<FileSplit> splits) {
        if (splits.isEmpty()) {
            return Optional.empty();
        }

        FileSplit firstSplit = splits.get(0);
        if (splits.size() == 1) {
            return Optional.of(firstSplit);
        }

        FileSplit lastSplit = splits.get(splits.size() - 1);

        return Optional.of(new FileSplit(
                firstSplit.getStartKey(),
                lastSplit.getStopKey(),
                firstSplit.getStartKeyOffset(),
                lastSplit.getStopKeyOffset()
        ));
    }

    public static VirtualSplitsBuilder builder() {
        return new VirtualSplitsBuilder();
    }
}
