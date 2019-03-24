package com.vd.diff.split.virtual;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode
public class VirtualSplit {

    private final String startKey;
    private final String stopKey;

    public VirtualSplit(String startKey, String stopKey) {
        if (startKey.compareTo(stopKey) > 0) {
            throw new IllegalArgumentException("Wrong start/stop keys: " + startKey + ":" + stopKey);
        }

        this.startKey = startKey;
        this.stopKey = stopKey;
    }

    public boolean isIntersects(VirtualSplit otherSplit) {
        return (startKey.compareTo(otherSplit.stopKey) < 0 && stopKey.compareTo(otherSplit.startKey) > 0);
    }
}
