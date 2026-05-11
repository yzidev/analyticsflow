package com.yzidev.analyticsflow.dto.response;

import java.time.Instant;

public record InvalidRecordResponse(
		String jobId,
		String sourceFile,
		String sourceTable,
		long rowNumber,
		String rawPayload,
		String errorMessage,
		Instant createdAt) {
}
