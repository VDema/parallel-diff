package com.vd.diff.file;

import com.vd.diff.content.Row;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class OffsetRow {
    private final long offset;
    private final Row row;

    public String getRowKey() {
        return row.getKey();
    }
}
