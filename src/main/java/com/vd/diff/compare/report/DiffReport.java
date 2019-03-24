package com.vd.diff.compare.report;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class DiffReport {
    private int leftSkippedRows;
    private int rightSkippedRows;
    private long timeElapsed;

    @Builder.Default
    private DiffMetrics metrics = new DiffMetrics();

    public DiffReport merge(DiffReport other) {
        return builder()
                .leftSkippedRows(leftSkippedRows + other.leftSkippedRows)
                .rightSkippedRows(rightSkippedRows + other.rightSkippedRows)
                .timeElapsed(timeElapsed + other.timeElapsed)
                .metrics(metrics.merge(other.metrics))
                .build();
    }
}
