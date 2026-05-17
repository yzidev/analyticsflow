package com.yzidev.analyticsflow.dto.request;

import java.time.LocalDate;

import com.yzidev.analyticsflow.common.enums.ReportFormat;
import com.yzidev.analyticsflow.common.enums.ReportStrategy;
import com.yzidev.analyticsflow.common.enums.ReportType;

import jakarta.validation.constraints.NotNull;

public record GenerateReportRequest(
		@NotNull ReportType reportType,
		LocalDate startDate,
		LocalDate endDate,
		@NotNull ReportFormat format,
		ReportStrategy strategy) {
}
