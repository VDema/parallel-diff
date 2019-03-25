package com.vd.diff.split;

import com.vd.diff.file.FastAccessRowFile;
import com.vd.diff.file.OffsetRow;
import com.vd.diff.split.file.FilePosition;
import com.vd.diff.split.file.FileSplit;
import com.vd.diff.split.file.FileSplits;
import com.vd.diff.split.virtual.VirtualFilePosition;
import com.vd.diff.split.virtual.VirtualSplit;
import com.vd.diff.split.virtual.VirtualSplits;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hadoop.hbase.util.RegionSplitter;

public class SplitService {

    public static FileSplits split(FastAccessRowFile file, int splitAmount) throws IOException {
        long fileLength = file.getFileSize();

        long byteSizeOffset = fileLength / splitAmount;

        List<FilePosition> filePositions = new ArrayList<>(splitAmount);

        for (int splitNum = 1; splitNum < splitAmount; splitNum++) {
            long splitOffset = byteSizeOffset * splitNum;
            OffsetRow row = file.nextRow(splitOffset);

            filePositions.add(new FilePosition(row.getRowKey(), row.getOffset()));
        }

        List<Pair<FilePosition, FilePosition>> splitPairs = convertToRangesInclusive(
                new FilePosition(file.getFirstRowKey(), 0L),
                new FilePosition(file.getLastRowKey(), fileLength),
                filePositions
        );

        return new FileSplits(splitPairs.stream()
                .map(SplitService::createFileSplit)
                .collect(Collectors.toList())
        );
    }

    public static VirtualSplits splitRange(String startKey, String stopKey, int splitsNum) {
        RegionSplitter.HexStringSplit alg = new RegionSplitter.HexStringSplit();

        alg.setFirstRow(startKey.replaceAll("-", "0"));
        alg.setLastRow(stopKey.replaceAll("-", "0"));

        List<VirtualFilePosition> splitPositions = Arrays.stream(alg.split(splitsNum))
                .map(SplitService::bytesToString)
                .map(VirtualFilePosition::new)
                .collect(Collectors.toList());

        List<Pair<VirtualFilePosition, VirtualFilePosition>> splitPairs = convertToRangesInclusive(
                new VirtualFilePosition(startKey),
                new VirtualFilePosition(stopKey),
                splitPositions);

        return new VirtualSplits(splitPairs.stream()
                .map(p -> new VirtualSplit(p.getLeft().getKey(), p.getRight().getKey()))
                .collect(Collectors.toList())
        );
    }

    private static FileSplit createFileSplit(Pair<FilePosition, FilePosition> splitPairs) {
        FilePosition leftPos = splitPairs.getLeft();
        FilePosition rightPos = splitPairs.getRight();

        return new FileSplit(
                leftPos.getKey(),
                rightPos.getKey(),
                leftPos.getKeyOffset(),
                rightPos.getKeyOffset());
    }

    private static <T extends VirtualFilePosition> List<Pair<T, T>> convertToRangesInclusive(
            T startPos,
            T stopPos,
            List<T> splitPositions) {

        if (splitPositions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Pair<T, T>> fromToPositions = new ArrayList<>();

        fromToPositions.add(Pair.of(startPos, splitPositions.get(0)));

        int lastSplitIdx = splitPositions.size() - 1;
        for (int splitKeyNum = 0; splitKeyNum < lastSplitIdx; splitKeyNum++) {
            T splitPos = splitPositions.get(splitKeyNum);
            T splitNextPos = splitPositions.get(splitKeyNum + 1);

            fromToPositions.add(Pair.of(splitPos, splitNextPos));
        }

        fromToPositions.add(Pair.of(splitPositions.get(lastSplitIdx), stopPos));

        return fromToPositions;
    }

    private static String bytesToString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
