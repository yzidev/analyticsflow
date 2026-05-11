package com.yzidev.analyticsflow.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ImportJobRequest(
		String sampleDirectory,
		@Min(100)
		@Max(100_000)
		Integer chunkSize) {
}
