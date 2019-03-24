package com.vd.diff.compare.report;

import lombok.ToString;

@ToString
public class DiffMetrics {
    int newLeftRowsAmount;
    int newRightRowsAmount;
    int differentEventsRowsAmount;

    public DiffMetrics merge(DiffMetrics otherMetrics) {
        DiffMetrics metrics = new DiffMetrics();
        metrics.newLeftRowsAmount = newLeftRowsAmount + otherMetrics.newLeftRowsAmount;
        metrics.newRightRowsAmount = newRightRowsAmount + otherMetrics.newRightRowsAmount;
        metrics.differentEventsRowsAmount = differentEventsRowsAmount + otherMetrics.differentEventsRowsAmount;
        return metrics;
    }

    public static class MetricsAggregator {
        DiffMetrics metrics = new DiffMetrics();

        public MetricsAggregator newLeft() {
            metrics.newLeftRowsAmount += 1;
            return this;
        }

        public MetricsAggregator newRight() {
            metrics.newRightRowsAmount += 1;
            return this;
        }

        public MetricsAggregator differentEvents() {
            metrics.differentEventsRowsAmount += 1;
            return this;
        }

        public DiffMetrics getMetrics() {
            return metrics;
        }
    }
}
