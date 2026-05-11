package com.yzidev.analyticsflow.controller;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yzidev.analyticsflow.common.enums.ReportFormat;
import com.yzidev.analyticsflow.dto.request.GenerateReportRequest;
import com.yzidev.analyticsflow.dto.response.ReportMetadataResponse;
import com.yzidev.analyticsflow.reporting.service.ReportExportService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/reports")
public class ReportController {

	private final ReportExportService reportExportService;

	public ReportController(ReportExportService reportExportService) {
		this.reportExportService = reportExportService;
	}

	@PostMapping("/generate")
	ReportMetadataResponse generate(@Valid @RequestBody GenerateReportRequest request) {
		return reportExportService.generate(request);
	}

	@GetMapping
	List<ReportMetadataResponse> reports() {
		return reportExportService.list();
	}

	@GetMapping("/{reportId}")
	ReportMetadataResponse report(@PathVariable String reportId) {
		return reportExportService.get(reportId);
	}

	@GetMapping("/{reportId}/download")
	ResponseEntity<Resource> download(@PathVariable String reportId) {
		ReportMetadataResponse metadata = reportExportService.get(reportId);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + metadata.fileName() + "\"")
				.contentType(mediaType(metadata.format()))
				.body(reportExportService.download(reportId));
	}

	private MediaType mediaType(ReportFormat format) {
		if (format == ReportFormat.CSV) {
			return MediaType.parseMediaType("text/csv");
		}
		return MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
	}
}
