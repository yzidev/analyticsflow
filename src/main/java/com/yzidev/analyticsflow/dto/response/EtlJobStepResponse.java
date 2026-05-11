package com.yzidev.analyticsflow.dto.response;

import java.time.Instant;

import com.yzidev.analyticsflow.common.enums.EtlJobStatus;

public record EtlJobStepResponse(
		String jobId,
		String stepName,
		EtlJobStatus status,
		long readCount,
		long writeCount,
		long skipCount,
		Instant startedAt,
		Instant finishedAt,
		Long durationMs,
		String errorMessage) {
}
