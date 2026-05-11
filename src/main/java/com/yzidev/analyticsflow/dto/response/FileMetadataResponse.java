package com.yzidev.analyticsflow.dto.response;

import java.time.Instant;
import java.util.List;

public record FileMetadataResponse(
		String fileName,
		String filePath,
		long sizeBytes,
		Instant lastModifiedAt,
		boolean readable,
		boolean csvExtension,
		List<String> header,
		boolean headerValid,
		List<String> errors) {
}
