package com.yzidev.analyticsflow.dto.response;

import com.yzidev.analyticsflow.common.enums.ReportFormat;
import com.yzidev.analyticsflow.common.enums.ReportStrategy;
import com.yzidev.analyticsflow.common.enums.ReportType;

public record ReportExportBenchmarkResponse(
		ReportStrategy strategy,
		ReportType reportType,
		ReportFormat format,
		long fileSizeBytes,
		long processingTimeMs,
		String threadName,
		String databaseProfile) {
}
