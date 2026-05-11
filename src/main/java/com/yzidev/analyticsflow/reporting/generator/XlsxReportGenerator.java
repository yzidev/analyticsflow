package com.yzidev.analyticsflow.reporting.generator;

import org.springframework.stereotype.Component;

import com.yzidev.analyticsflow.common.enums.ReportType;

@Component
public class XlsxReportGenerator {

	public byte[] generatePlaceholder(ReportType reportType) {
		String content = "XLSX placeholder for " + reportType
				+ ". Add Apache POI when binary workbook export becomes part of the next phase.";
		return content.getBytes();
	}
}
