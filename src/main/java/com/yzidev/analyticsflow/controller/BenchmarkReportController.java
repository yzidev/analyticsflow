package com.yzidev.analyticsflow.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yzidev.analyticsflow.common.enums.ReportFormat;
import com.yzidev.analyticsflow.common.enums.ReportStrategy;
import com.yzidev.analyticsflow.common.enums.ReportType;
import com.yzidev.analyticsflow.dto.response.ReportExportBenchmarkResponse;
import com.yzidev.analyticsflow.dto.response.ReportBenchmarkResponse;
import com.yzidev.analyticsflow.reporting.service.ReportExportService;
import com.yzidev.analyticsflow.reporting.service.BlockingReportService;
import com.yzidev.analyticsflow.reporting.service.CompletableFutureReportService;
import com.yzidev.analyticsflow.reporting.service.ReactiveReportService;
import com.yzidev.analyticsflow.reporting.service.VirtualThreadReportService;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/benchmark/reports")
public class BenchmarkReportController {

	private final BlockingReportService blockingReportService;
	private final VirtualThreadReportService virtualThreadReportService;
	private final CompletableFutureReportService completableFutureReportService;
	private final ReactiveReportService reactiveReportService;
	private final ReportExportService reportExportService;

	public BenchmarkReportController(
			BlockingReportService blockingReportService,
			VirtualThreadReportService virtualThreadReportService,
			CompletableFutureReportService completableFutureReportService,
			ReactiveReportService reactiveReportService,
			ReportExportService reportExportService) {
		this.blockingReportService = blockingReportService;
		this.virtualThreadReportService = virtualThreadReportService;
		this.completableFutureReportService = completableFutureReportService;
		this.reactiveReportService = reactiveReportService;
		this.reportExportService = reportExportService;
	}

	@GetMapping("/blocking")
	ReportBenchmarkResponse blocking() {
		return blockingReportService.benchmark();
	}

	@GetMapping("/virtual-thread")
	ReportBenchmarkResponse virtualThread() {
		return virtualThreadReportService.benchmark();
	}

	@GetMapping("/completable-future")
	ReportBenchmarkResponse completableFuture() {
		return completableFutureReportService.benchmark();
	}

	@GetMapping("/reactive")
	Mono<ReportBenchmarkResponse> reactive() {
		return reactiveReportService.benchmark();
	}

	@GetMapping("/exports/{strategy}")
	ReportExportBenchmarkResponse export(
			@PathVariable String strategy,
			@RequestParam(defaultValue = "SALES_PRODUCT_SUMMARY") ReportType reportType,
			@RequestParam(defaultValue = "CSV") ReportFormat format) {
		return reportExportService.benchmarkGenerate(reportStrategy(strategy), reportType, format);
	}

	private ReportStrategy reportStrategy(String value) {
		return ReportStrategy.valueOf(value.replace("-", "_").toUpperCase());
	}
}
