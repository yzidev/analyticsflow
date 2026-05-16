package com.yzidev.analyticsflow.job.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yzidev.analyticsflow.common.enums.EtlJobStatus;
import com.yzidev.analyticsflow.common.enums.EtlStepName;
import com.yzidev.analyticsflow.common.exception.BadRequestException;
import com.yzidev.analyticsflow.common.exception.ResourceNotFoundException;
import com.yzidev.analyticsflow.config.AnalyticsFlowProperties;
import com.yzidev.analyticsflow.dto.request.ImportJobRequest;
import com.yzidev.analyticsflow.dto.response.EtlJobResponse;
import com.yzidev.analyticsflow.dto.response.EtlJobStepResponse;
import com.yzidev.analyticsflow.dto.response.FileValidationResponse;
import com.yzidev.analyticsflow.dto.response.InvalidRecordResponse;
import com.yzidev.analyticsflow.entity.support.EtlJobEntity;
import com.yzidev.analyticsflow.entity.support.EtlJobStepEntity;
import com.yzidev.analyticsflow.entity.support.InvalidRecordEntity;
import com.yzidev.analyticsflow.observability.AnalyticsFlowMetrics;
import com.yzidev.analyticsflow.ingestion.validator.CsvFileValidator;
import com.yzidev.analyticsflow.repository.jpa.support.EtlJobRepository;
import com.yzidev.analyticsflow.repository.jpa.support.EtlJobStepRepository;
import com.yzidev.analyticsflow.repository.jpa.support.InvalidRecordRepository;

@Service
public class JobMonitoringService {

	private final JobLauncher jobLauncher;
	private final Job analyticsFlowEtlJob;
	private final CsvFileValidator csvFileValidator;
	private final AnalyticsFlowProperties properties;
	private final Executor executor;
	private final EtlJobRepository etlJobRepository;
	private final EtlJobStepRepository etlJobStepRepository;
	private final InvalidRecordRepository invalidRecordRepository;
	private final AnalyticsFlowMetrics metrics;

	public JobMonitoringService(
			JobLauncher jobLauncher,
			Job analyticsFlowEtlJob,
			CsvFileValidator csvFileValidator,
			AnalyticsFlowProperties properties,
			@Qualifier("analyticsTaskExecutor") Executor executor,
			EtlJobRepository etlJobRepository,
			EtlJobStepRepository etlJobStepRepository,
			InvalidRecordRepository invalidRecordRepository,
			AnalyticsFlowMetrics metrics) {
		this.jobLauncher = jobLauncher;
		this.analyticsFlowEtlJob = analyticsFlowEtlJob;
		this.csvFileValidator = csvFileValidator;
		this.properties = properties;
		this.executor = executor;
		this.etlJobRepository = etlJobRepository;
		this.etlJobStepRepository = etlJobStepRepository;
		this.invalidRecordRepository = invalidRecordRepository;
		this.metrics = metrics;
	}

	public EtlJobResponse startImport(ImportJobRequest request) {
		int chunkSize = request.chunkSize() == null ? properties.batch().defaultChunkSize() : request.chunkSize();
		if (chunkSize <= 0) {
			throw new BadRequestException("chunkSize must be greater than zero");
		}

		String sampleDirectory = request.sampleDirectory() == null || request.sampleDirectory().isBlank()
				? properties.data().sourceDirectory()
				: request.sampleDirectory();

		String jobId = UUID.randomUUID().toString();
		EtlJobEntity job = createPendingJob(jobId, sampleDirectory);
		metrics.jobStarted();

		FileValidationResponse validation = csvFileValidator.validate(sampleDirectory);
		if (!validation.valid()) {
			job.setStatus(EtlJobStatus.FAILED);
			job.setStartedAt(LocalDateTime.now());
			job.setFinishedAt(LocalDateTime.now());
			job.setDurationMs(Duration.between(job.getStartedAt(), job.getFinishedAt()).toMillis());
			job.setErrorMessage("CSV validation failed before import");
			saveValidationErrors(jobId, validation);
			EtlJobEntity saved = etlJobRepository.save(job);
			metrics.jobFinished(saved.getStatus(), value(saved.getProcessedRows()), saved.getDurationMs());
			return toJobResponse(saved);
		}

		JobParameters jobParameters = new JobParametersBuilder()
				.addString("jobId", jobId)
				.addString("sampleDirectory", sampleDirectory)
				.addLong("chunkSize", (long) chunkSize)
				.addLocalDateTime("requestedAt", LocalDateTime.now())
				.toJobParameters();

		CompletableFuture.runAsync(() -> launch(jobId, jobParameters), executor);
		return toJobResponse(job);
	}

	public List<EtlJobResponse> listJobs() {
		return etlJobRepository.findAll().stream()
				.sorted(Comparator.comparing(EtlJobEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
				.map(this::toJobResponse)
				.toList();
	}

	public EtlJobResponse getJob(String jobId) {
		return toJobResponse(findJob(jobId));
	}

	public List<EtlJobStepResponse> getSteps(String jobId) {
		if (!etlJobRepository.existsByJobId(jobId)) {
			throw new ResourceNotFoundException("Job not found: " + jobId);
		}
		return etlJobStepRepository.findByJobIdOrderByIdAsc(jobId).stream()
				.map(this::toStepResponse)
				.toList();
	}

	public List<InvalidRecordResponse> getErrors(String jobId) {
		if (!etlJobRepository.existsByJobId(jobId)) {
			throw new ResourceNotFoundException("Job not found: " + jobId);
		}
		return invalidRecordRepository.findByJobId(jobId).stream()
				.map(this::toInvalidRecordResponse)
				.toList();
	}

	private EtlJobEntity createPendingJob(String jobId, String sampleDirectory) {
		EtlJobEntity job = new EtlJobEntity();
		job.setJobId(jobId);
		job.setSampleDirectory(sampleDirectory);
		job.setStatus(EtlJobStatus.PENDING);
		job.setTotalRows(0L);
		job.setProcessedRows(0L);
		job.setSuccessRows(0L);
		job.setFailedRows(0L);
		return etlJobRepository.save(job);
	}

	private void launch(String jobId, JobParameters jobParameters) {
		try {
			jobLauncher.run(analyticsFlowEtlJob, jobParameters);
		}
		catch (Exception exception) {
			markJobFailed(jobId, exception);
		}
	}

	@Transactional(transactionManager = "transactionManager")
	protected void markJobFailed(String jobId, Exception exception) {
		EtlJobEntity job = findJob(jobId);
		job.setStatus(EtlJobStatus.FAILED);
		job.setFinishedAt(LocalDateTime.now());
		job.setErrorMessage(exception.getMessage());
		job.setUpdatedAt(LocalDateTime.now());
		EtlJobEntity saved = etlJobRepository.save(job);
		metrics.jobFinished(saved.getStatus(), value(saved.getProcessedRows()), saved.getDurationMs());
	}

	private EtlJobEntity findJob(String jobId) {
		return etlJobRepository.findByJobId(jobId)
				.orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));
	}

	private EtlJobResponse toJobResponse(EtlJobEntity job) {
		List<EtlJobStepEntity> steps = etlJobStepRepository.findByJobIdOrderByIdAsc(job.getJobId());
		long stepReadRows = steps.stream().mapToLong(step -> value(step.getReadCount())).sum();
		long totalRows = Math.max(value(job.getTotalRows()), stepReadRows);
		long processedRows = Math.max(value(job.getProcessedRows()), stepReadRows);
		long successRows = Math.max(value(job.getSuccessRows()), steps.stream().mapToLong(step -> value(step.getWriteCount())).sum());
		long failedRows = Math.max(value(job.getFailedRows()), steps.stream().mapToLong(step -> value(step.getSkipCount())).sum());
		int progressPercent = progressPercent(job, steps, totalRows, processedRows);
		return new EtlJobResponse(
				job.getJobId(),
				job.getSampleDirectory(),
				job.getStatus(),
				totalRows,
				processedRows,
				successRows,
				failedRows,
				progressPercent,
				toInstant(job.getStartedAt()),
				toInstant(job.getFinishedAt()),
				job.getDurationMs(),
				job.getErrorMessage());
	}

	private int progressPercent(EtlJobEntity job, List<EtlJobStepEntity> steps, long totalRows, long processedRows) {
		if (job.getStatus() == EtlJobStatus.COMPLETED) {
			return 100;
		}
		if (totalRows > 0 && processedRows < totalRows) {
			return (int) Math.min(99, Math.round((processedRows * 100.0) / totalRows));
		}
		if (steps.isEmpty()) {
			return 0;
		}
		long completedSteps = steps.stream()
				.filter(step -> step.getStatus() == EtlJobStatus.COMPLETED)
				.count();
		boolean hasRunningStep = steps.stream().anyMatch(step -> step.getStatus() == EtlJobStatus.RUNNING);
		double currentStepCredit = hasRunningStep ? 0.5 : 0.0;
		return (int) Math.min(99,
				Math.round(((completedSteps + currentStepCredit) * 100.0) / EtlStepName.values().length));
	}

	private EtlJobStepResponse toStepResponse(EtlJobStepEntity step) {
		return new EtlJobStepResponse(
				step.getJobId(),
				step.getStepName().name(),
				step.getStatus(),
				value(step.getReadCount()),
				value(step.getWriteCount()),
				value(step.getSkipCount()),
				toInstant(step.getStartedAt()),
				toInstant(step.getFinishedAt()),
				step.getDurationMs(),
				step.getErrorMessage());
	}

	private InvalidRecordResponse toInvalidRecordResponse(InvalidRecordEntity invalidRecord) {
		return new InvalidRecordResponse(
				invalidRecord.getJobId(),
				invalidRecord.getSourceFile(),
				invalidRecord.getSourceTable(),
				value(invalidRecord.getRowNumber()),
				invalidRecord.getRawPayload(),
				invalidRecord.getErrorMessage(),
				toInstant(invalidRecord.getCreatedAt()));
	}

	private long value(Long value) {
		return value == null ? 0 : value;
	}

	private void saveValidationErrors(String jobId, FileValidationResponse validation) {
		validation.errors().forEach(error -> saveInvalidRecord(jobId, "validation", error));
		validation.missingFiles().forEach(file -> saveInvalidRecord(jobId, file, "Required CSV file is missing"));
		validation.invalidHeaders().forEach(file -> saveInvalidRecord(jobId, file, "CSV header does not match expected structure"));
	}

	private void saveInvalidRecord(String jobId, String sourceFile, String errorMessage) {
		InvalidRecordEntity invalidRecord = new InvalidRecordEntity();
		invalidRecord.setJobId(jobId);
		invalidRecord.setSourceFile(sourceFile);
		invalidRecord.setSourceTable("file_validation");
		invalidRecord.setErrorMessage(errorMessage);
		invalidRecord.setCreatedAt(LocalDateTime.now());
		invalidRecordRepository.save(invalidRecord);
	}

	private java.time.Instant toInstant(LocalDateTime value) {
		return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
	}
}
