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

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yzidev.analyticsflow.common.enums.ReportGenerationStatus;
import com.yzidev.analyticsflow.common.enums.ReportFormat;
import com.yzidev.analyticsflow.common.enums.ReportType;
import com.yzidev.analyticsflow.common.exception.BadRequestException;
import com.yzidev.analyticsflow.common.exception.ResourceNotFoundException;
import com.yzidev.analyticsflow.config.AnalyticsFlowProperties;
import com.yzidev.analyticsflow.dto.request.GenerateReportRequest;
import com.yzidev.analyticsflow.dto.response.ReportMetadataResponse;
import com.yzidev.analyticsflow.entity.support.ReportMetadataEntity;
import com.yzidev.analyticsflow.observability.AnalyticsFlowMetrics;
import com.yzidev.analyticsflow.reporting.generator.CsvReportGenerator;
import com.yzidev.analyticsflow.reporting.generator.XlsxReportGenerator;
import com.yzidev.analyticsflow.repository.jpa.support.ReportMetadataRepository;

@Service
public class ReportExportService {

	private final AnalyticsFlowProperties properties;
	private final CsvReportGenerator csvReportGenerator;
	private final XlsxReportGenerator xlsxReportGenerator;
	private final ReportMetadataRepository reportMetadataRepository;
	private final AnalyticsFlowMetrics metrics;

	public ReportExportService(
			AnalyticsFlowProperties properties,
			CsvReportGenerator csvReportGenerator,
			XlsxReportGenerator xlsxReportGenerator,
			ReportMetadataRepository reportMetadataRepository,
			AnalyticsFlowMetrics metrics) {
		this.properties = properties;
		this.csvReportGenerator = csvReportGenerator;
		this.xlsxReportGenerator = xlsxReportGenerator;
		this.reportMetadataRepository = reportMetadataRepository;
		this.metrics = metrics;
	}

	@Transactional(transactionManager = "transactionManager")
	public ReportMetadataResponse generate(GenerateReportRequest request) {
		return generate(request, null);
	}

	@Transactional(transactionManager = "transactionManager")
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
			if (request.format() == ReportFormat.CSV) {
				csvReportGenerator.generate(reportPath, request.reportType(), request.startDate(), request.endDate());
			}
			else {
				Files.write(reportPath, xlsxReportGenerator.generatePlaceholder(request.reportType()));
			}
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
				.map(reportType -> generate(new GenerateReportRequest(reportType, null, null, ReportFormat.CSV), jobId))
				.toList();
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
}
