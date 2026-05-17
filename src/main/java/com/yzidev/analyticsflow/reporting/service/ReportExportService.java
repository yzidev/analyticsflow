package com.yzidev.analyticsflow.reporting.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yzidev.analyticsflow.common.enums.ReportGenerationStatus;
import com.yzidev.analyticsflow.common.enums.ReportFormat;
import com.yzidev.analyticsflow.common.enums.ReportStrategy;
import com.yzidev.analyticsflow.common.enums.ReportType;
import com.yzidev.analyticsflow.common.exception.BadRequestException;
import com.yzidev.analyticsflow.common.exception.ResourceNotFoundException;
import com.yzidev.analyticsflow.config.AnalyticsFlowProperties;
import com.yzidev.analyticsflow.dto.request.GenerateReportRequest;
import com.yzidev.analyticsflow.dto.response.ReportExportBenchmarkResponse;
import com.yzidev.analyticsflow.dto.response.ReportMetadataResponse;
import com.yzidev.analyticsflow.entity.support.ReportMetadataEntity;
import com.yzidev.analyticsflow.observability.AnalyticsFlowMetrics;
import com.yzidev.analyticsflow.reporting.generator.CsvReportGenerator;
import com.yzidev.analyticsflow.reporting.generator.XlsxReportGenerator;
import com.yzidev.analyticsflow.repository.jpa.support.ReportMetadataRepository;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ReportExportService {

	private final AnalyticsFlowProperties properties;
	private final CsvReportGenerator csvReportGenerator;
	private final XlsxReportGenerator xlsxReportGenerator;
	private final ReportMetadataRepository reportMetadataRepository;
	private final AnalyticsFlowMetrics metrics;
	private final Executor completableFutureExecutor;
	private final ExecutorService virtualThreadExecutor;

	public ReportExportService(
			AnalyticsFlowProperties properties,
			CsvReportGenerator csvReportGenerator,
			XlsxReportGenerator xlsxReportGenerator,
			ReportMetadataRepository reportMetadataRepository,
			AnalyticsFlowMetrics metrics,
			@Qualifier("analyticsCompletableFutureExecutor") Executor completableFutureExecutor,
			@Qualifier("analyticsVirtualThreadExecutor") ExecutorService virtualThreadExecutor) {
		this.properties = properties;
		this.csvReportGenerator = csvReportGenerator;
		this.xlsxReportGenerator = xlsxReportGenerator;
		this.reportMetadataRepository = reportMetadataRepository;
		this.metrics = metrics;
		this.completableFutureExecutor = completableFutureExecutor;
		this.virtualThreadExecutor = virtualThreadExecutor;
	}

	public ReportMetadataResponse generate(GenerateReportRequest request) {
		return generate(request, null);
	}

	public ReportMetadataResponse generate(GenerateReportRequest request, String jobId) {
		validateRequest(request);
		Instant startedAt = Instant.now();
		String reportId = UUID.randomUUID().toString();
		String extension = request.format().name().toLowerCase();
		String fileName = request.reportType().name().toLowerCase() + "-" + reportId + "." + extension;
		Path reportPath = reportsDirectory().resolve(fileName).normalize();
		ReportMetadataEntity metadata = pendingMetadata(reportId, jobId, request.reportType(), fileName, reportPath,
				request.format());

		try {
			reportMetadataRepository.save(metadata);
			Files.createDirectories(reportPath.getParent());
			generateWithStrategy(reportPath, request.reportType(), request.startDate(), request.endDate(),
					request.format(), strategy(request.strategy()));
			metadata.setStatus(ReportGenerationStatus.GENERATED);
			metadata.setGeneratedAt(LocalDateTime.now());
		}
		catch (IOException exception) {
			metadata.setStatus(ReportGenerationStatus.FAILED);
			metadata.setDurationMs(Duration.between(startedAt, Instant.now()).toMillis());
			metadata.setUpdatedAt(LocalDateTime.now());
			reportMetadataRepository.save(metadata);
			metrics.reportGenerated(metadata.getReportType(), metadata.getStatus(), metadata.getDurationMs());
			throw new BadRequestException("Cannot generate report file: " + exception.getMessage());
		}
		catch (RuntimeException exception) {
			metadata.setStatus(ReportGenerationStatus.FAILED);
			metadata.setDurationMs(Duration.between(startedAt, Instant.now()).toMillis());
			metadata.setUpdatedAt(LocalDateTime.now());
			reportMetadataRepository.save(metadata);
			metrics.reportGenerated(metadata.getReportType(), metadata.getStatus(), metadata.getDurationMs());
			throw exception;
		}

		metadata.setDurationMs(Duration.between(startedAt, Instant.now()).toMillis());
		metadata.setUpdatedAt(LocalDateTime.now());
		ReportMetadataEntity saved = reportMetadataRepository.save(metadata);
		metrics.reportGenerated(saved.getReportType(), saved.getStatus(), saved.getDurationMs());
		return toResponse(saved);
	}

	@Transactional(transactionManager = "transactionManager")
	public List<ReportMetadataResponse> generateInitialReports(String jobId) {
		return java.util.Arrays.stream(ReportType.values())
				.map(reportType -> generate(new GenerateReportRequest(reportType, null, null, ReportFormat.CSV, null), jobId))
				.toList();
	}

	public ReportExportBenchmarkResponse benchmarkGenerate(
			ReportStrategy strategy,
			ReportType reportType,
			ReportFormat format) {
		ReportStrategy selectedStrategy = strategy(strategy);
		ReportType selectedReportType = reportType == null ? ReportType.SALES_PRODUCT_SUMMARY : reportType;
		ReportFormat selectedFormat = format == null ? ReportFormat.CSV : format;
		Instant startedAt = Instant.now();
		Path reportPath = null;
		try {
			Files.createDirectories(reportsDirectory());
			reportPath = Files.createTempFile(reportsDirectory(),
					"benchmark-" + selectedStrategy.name().toLowerCase() + "-",
					"." + selectedFormat.name().toLowerCase());
			generateWithStrategy(reportPath, selectedReportType, null, null, selectedFormat, selectedStrategy);
			return new ReportExportBenchmarkResponse(
					selectedStrategy,
					selectedReportType,
					selectedFormat,
					Files.size(reportPath),
					Duration.between(startedAt, Instant.now()).toMillis(),
					Thread.currentThread().getName(),
					"postgresql");
		}
		catch (IOException exception) {
			throw new BadRequestException("Cannot benchmark report generation: " + exception.getMessage());
		}
		finally {
			deleteIfExists(reportPath);
		}
	}

	public List<ReportMetadataResponse> list() {
		return reportMetadataRepository.findAll().stream()
				.sorted(Comparator.comparing(ReportMetadataEntity::getGeneratedAt,
						Comparator.nullsLast(Comparator.reverseOrder())))
				.map(this::toResponse)
				.toList();
	}

	public ReportMetadataResponse get(String reportId) {
		return toResponse(findReport(reportId));
	}

	public Resource download(String reportId) {
		ReportMetadataEntity metadata = findReport(reportId);
		Path path = Path.of(metadata.getFilePath()).toAbsolutePath().normalize();
		if (!path.startsWith(reportsDirectory()) || !Files.exists(path)) {
			throw new ResourceNotFoundException("Report file not found: " + reportId);
		}
		return new FileSystemResource(path);
	}

	private Path reportsDirectory() {
		return Path.of(properties.data().reportsDirectory()).toAbsolutePath().normalize();
	}

	private void generateWithStrategy(
			Path reportPath,
			ReportType reportType,
			java.time.LocalDate startDate,
			java.time.LocalDate endDate,
			ReportFormat format,
			ReportStrategy strategy) throws IOException {
		switch (strategy) {
			case BLOCKING -> generateFile(reportPath, reportType, startDate, endDate, format);
			case VIRTUAL_THREAD -> runVirtualThread(() -> generateFile(reportPath, reportType, startDate, endDate, format));
			case COMPLETABLE_FUTURE -> runCompletableFuture(
					() -> generateFile(reportPath, reportType, startDate, endDate, format));
			case REACTIVE -> runReactive(() -> generateFile(reportPath, reportType, startDate, endDate, format));
		}
	}

	private void generateFile(
			Path reportPath,
			ReportType reportType,
			java.time.LocalDate startDate,
			java.time.LocalDate endDate,
			ReportFormat format) throws IOException {
		if (format == ReportFormat.CSV) {
			csvReportGenerator.generate(reportPath, reportType, startDate, endDate);
		}
		else {
			Files.write(reportPath, xlsxReportGenerator.generatePlaceholder(reportType));
		}
	}

	private void runVirtualThread(ReportGenerationOperation operation) throws IOException {
		try {
			virtualThreadExecutor.submit(() -> {
				operation.run();
				return null;
			}).get();
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IOException("Virtual thread report generation interrupted", exception);
		}
		catch (ExecutionException exception) {
			throw unwrapGenerationException(exception.getCause());
		}
	}

	private void runCompletableFuture(ReportGenerationOperation operation) throws IOException {
		try {
			CompletableFuture.runAsync(() -> {
				try {
					operation.run();
				}
				catch (IOException exception) {
					throw new CompletionException(exception);
				}
			}, completableFutureExecutor).join();
		}
		catch (CompletionException exception) {
			throw unwrapGenerationException(exception.getCause());
		}
	}

	private void runReactive(ReportGenerationOperation operation) throws IOException {
		try {
			Mono.fromRunnable(() -> {
				try {
					operation.run();
				}
				catch (IOException exception) {
					throw new CompletionException(exception);
				}
			})
					.subscribeOn(Schedulers.boundedElastic())
					.block();
		}
		catch (CompletionException exception) {
			throw unwrapGenerationException(exception.getCause());
		}
	}

	private IOException unwrapGenerationException(Throwable exception) {
		Throwable current = exception;
		while (current instanceof CompletionException && current.getCause() != null) {
			current = current.getCause();
		}
		if (current instanceof IOException ioException) {
			return ioException;
		}
		return new IOException("Report generation failed: " + current.getMessage(), current);
	}

	private ReportStrategy strategy(ReportStrategy strategy) {
		return strategy == null ? ReportStrategy.BLOCKING : strategy;
	}

	private void deleteIfExists(Path path) {
		if (path == null) {
			return;
		}
		try {
			Files.deleteIfExists(path);
		}
		catch (IOException ignored) {
			// Benchmark temp files should not affect the benchmark response.
		}
	}

	private void validateRequest(GenerateReportRequest request) {
		if (request.startDate() != null && request.endDate() != null && request.startDate().isAfter(request.endDate())) {
			throw new BadRequestException("startDate must be on or before endDate");
		}
	}

	private ReportMetadataEntity pendingMetadata(
			String reportId,
			String jobId,
			ReportType reportType,
			String fileName,
			Path reportPath,
			ReportFormat format) {
		ReportMetadataEntity metadata = new ReportMetadataEntity();
		metadata.setReportId(reportId);
		metadata.setJobId(jobId);
		metadata.setReportType(reportType);
		metadata.setFileName(fileName);
		metadata.setFilePath(reportPath.toString());
		metadata.setFormat(format);
		metadata.setStatus(ReportGenerationStatus.GENERATING);
		metadata.setCreatedAt(LocalDateTime.now());
		metadata.setUpdatedAt(LocalDateTime.now());
		return metadata;
	}

	private ReportMetadataEntity findReport(String reportId) {
		return reportMetadataRepository.findByReportId(reportId)
				.orElseThrow(() -> new ResourceNotFoundException("Report not found: " + reportId));
	}

	private ReportMetadataResponse toResponse(ReportMetadataEntity metadata) {
		return new ReportMetadataResponse(
				metadata.getReportId(),
				metadata.getJobId(),
				metadata.getReportType(),
				metadata.getFileName(),
				metadata.getFilePath(),
				metadata.getFormat(),
				metadata.getStatus().name(),
				toInstant(metadata.getGeneratedAt()),
				metadata.getDurationMs() == null ? 0L : metadata.getDurationMs(),
				"/api/reports/" + metadata.getReportId() + "/download");
	}

	private Instant toInstant(LocalDateTime value) {
		return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
	}

	@FunctionalInterface
	private interface ReportGenerationOperation {

		void run() throws IOException;
	}
}
