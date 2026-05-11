package com.yzidev.analyticsflow.dto.response;

import java.time.Instant;

import com.yzidev.analyticsflow.common.enums.ReportFormat;
import com.yzidev.analyticsflow.common.enums.ReportType;

public record ReportMetadataResponse(
		String reportId,
		String jobId,
		ReportType reportType,
		String fileName,
		String filePath,
		ReportFormat format,
		String status,
		Instant generatedAt,
		long durationMs,
		String downloadUrl) {
}
