package com.yzidev.analyticsflow.dto.response;

import java.time.Instant;

import com.yzidev.analyticsflow.common.enums.EtlJobStatus;

public record EtlJobResponse(
		String jobId,
		String sampleDirectory,
		EtlJobStatus status,
		long totalRows,
		long processedRows,
		long successRows,
		long failedRows,
		int progressPercent,
		Instant startedAt,
		Instant finishedAt,
		Long durationMs,
		String errorMessage) {
}
