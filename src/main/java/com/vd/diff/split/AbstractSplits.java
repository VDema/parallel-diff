package com.vd.diff.split;

import com.vd.diff.split.virtual.VirtualSplit;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AbstractSplits<T extends VirtualSplit> {

    protected final List<T> splits;

}
