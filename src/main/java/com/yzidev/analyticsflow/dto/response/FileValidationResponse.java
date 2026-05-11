package com.yzidev.analyticsflow.dto.response;

import java.util.List;

public record FileValidationResponse(
		boolean valid,
		List<String> missingFiles,
		List<String> invalidHeaders,
		List<String> availableFiles,
		List<FileMetadataResponse> files,
		List<String> errors) {
}
