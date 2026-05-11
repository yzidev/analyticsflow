package com.yzidev.analyticsflow.batch.migration;

public record MigrationResult(long readCount, long writeCount, long invalidCount) {
}
