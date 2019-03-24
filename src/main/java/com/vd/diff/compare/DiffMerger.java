package com.vd.diff.compare;

import com.google.common.base.Preconditions;
import com.vd.diff.compare.task.DiffTask;
import com.vd.diff.file.FastAccessFile;
import com.vd.diff.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class DiffMerger {

    File merge(List<DiffTask.DiffResult> diffResults, String outFileName) throws IOException {
        File destination = FileUtils.createNew(outFileName);

        List<DiffTask.DiffResult> sortedDiffs = diffResults.stream()
                .sorted(Comparator.comparing(DiffTask.DiffResult::getDiffOrderNum))
                .collect(Collectors.toList());

        try (FastAccessFile fastAccessDest = new FastAccessFile(destination, "rw")) {
            FileChannel destinationChannel = fastAccessDest.getChannel();
            for (DiffTask.DiffResult diff : sortedDiffs) {
                try (FastAccessFile fastAccessDiff = new FastAccessFile(diff.getDiffFile(), "rw")) {
                    transferAll(fastAccessDiff, destinationChannel, destination.getPath());
                }
            }
            return destination;
        }
    }

    private void transferAll(FastAccessFile from, FileChannel to, String toPath) {
        try {
            FileChannel fromChannel = from.getChannel();

            if (fromChannel.position() != 0) {
                fromChannel.position(0);
            }

            long byteTransferred = fromChannel.transferTo(0, from.length(), to);

            Preconditions.checkState(byteTransferred == from.length());

        } catch (IOException e) {
            throw new RuntimeException(String.format(
                    "Unable to transfer data from: [%s], to: [%s]", from.getPath(), toPath));
        }
    }
}
