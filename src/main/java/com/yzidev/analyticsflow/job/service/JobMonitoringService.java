package com.yzidev.analyticsflow.job.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yzidev.analyticsflow.common.enums.EtlJobStatus;
import com.yzidev.analyticsflow.common.enums.EtlStepName;
import com.yzidev.analyticsflow.common.enums.StagingWriterStrategy;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(JobMonitoringService.class);
	private static final String ETL_JOB_NAME = "analyticsflow-etl-job";
	private static final int JOB_INSTANCE_PAGE_SIZE = 100;

	private final JobLauncher jobLauncher;
	private final JobRepository batchJobRepository;
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
			JobRepository batchJobRepository,
			Job analyticsFlowEtlJob,
			CsvFileValidator csvFileValidator,
			AnalyticsFlowProperties properties,
			@Qualifier("analyticsTaskExecutor") Executor executor,
			EtlJobRepository etlJobRepository,
			EtlJobStepRepository etlJobStepRepository,
			InvalidRecordRepository invalidRecordRepository,
			AnalyticsFlowMetrics metrics) {
		this.jobLauncher = jobLauncher;
		this.batchJobRepository = batchJobRepository;
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
		StagingWriterStrategy writerStrategy = writerStrategy(request.writerStrategy());

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
				.addString("jobId", jobId, true)
				.addString("sampleDirectory", sampleDirectory, true)
				.addString("writerStrategy", writerStrategy.name(), false)
				.addLong("chunkSize", (long) chunkSize, false)
				.addLocalDateTime("requestedAt", LocalDateTime.now(), false)
				.toJobParameters();

		CompletableFuture.runAsync(() -> launch(jobId, jobParameters), executor);
		LOGGER.info(
				"etl_job_queued jobId={} sampleDirectory={} writerStrategy={} chunkSize={}",
				jobId,
				sampleDirectory,
				writerStrategy.name(),
				chunkSize);
		return toJobResponse(job);
	}

	public EtlJobResponse resumeImport(String jobId, ImportJobRequest request) {
		EtlJobEntity job = findJob(jobId);
		Optional<JobExecution> lastExecution = latestBatchExecution(jobId);
		if (lastExecution.isEmpty()) {
			return resumeWithoutBatchMetadata(job, request);
		}
		return resumeFromBatchExecution(job, lastExecution.get(), request);
	}

	private EtlJobResponse resumeFromBatchExecution(
			EtlJobEntity job,
			JobExecution lastExecution,
			ImportJobRequest request) {
		String jobId = job.getJobId();
		if (lastExecution.getStatus() == BatchStatus.COMPLETED) {
			throw new BadRequestException("Job is already completed: " + jobId);
		}
		if (lastExecution.getStatus() == BatchStatus.ABANDONED) {
			throw new BadRequestException("Job execution is abandoned and cannot be resumed: " + jobId);
		}

		JobExecution restartableExecution = recoverIfStale(lastExecution);
		JobParameters jobParameters = resumeParameters(restartableExecution.getJobParameters(), request);
		job.setStatus(EtlJobStatus.RUNNING);
		job.setFinishedAt(null);
		job.setDurationMs(null);
		job.setErrorMessage(null);
		job.setUpdatedAt(LocalDateTime.now());
		EtlJobEntity saved = etlJobRepository.save(job);
		metrics.jobStarted();

		CompletableFuture.runAsync(() -> launch(jobId, jobParameters), executor);
		LOGGER.info(
				"etl_job_resume_queued jobId={} previousExecutionId={} writerStrategy={} chunkSize={}",
				jobId,
				restartableExecution.getId(),
				jobParameters.getString("writerStrategy", "JPA"),
				jobParameters.getLong("chunkSize", 0L));
		return toJobResponse(saved);
	}

	private EtlJobResponse resumeWithoutBatchMetadata(EtlJobEntity job, ImportJobRequest request) {
		String jobId = job.getJobId();
		JobParameters jobParameters = fallbackResumeParameters(job, request);
		job.setStatus(EtlJobStatus.RUNNING);
		job.setFinishedAt(null);
		job.setDurationMs(null);
		job.setErrorMessage(null);
		job.setUpdatedAt(LocalDateTime.now());
		EtlJobEntity saved = etlJobRepository.save(job);
		metrics.jobStarted();

		CompletableFuture.runAsync(() -> launch(jobId, jobParameters), executor);
		LOGGER.info(
				"etl_job_resume_fallback_queued jobId={} reason=batch_metadata_not_found sampleDirectory={} writerStrategy={} chunkSize={}",
				jobId,
				jobParameters.getString("sampleDirectory", job.getSampleDirectory()),
				jobParameters.getString("writerStrategy", "JPA"),
				jobParameters.getLong("chunkSize", 0L));
		return toJobResponse(saved);
	}

	private StagingWriterStrategy writerStrategy(String value) {
		try {
			return StagingWriterStrategy.from(value);
		}
		catch (IllegalArgumentException exception) {
			throw new BadRequestException(exception.getMessage());
		}
	}

	private JobParameters resumeParameters(JobParameters previous, ImportJobRequest request) {
		JobParametersBuilder builder = new JobParametersBuilder(previous);
		if (request == null) {
			return builder.toJobParameters();
		}
		if (request.sampleDirectory() != null && !request.sampleDirectory().isBlank()
				&& !request.sampleDirectory().equals(previous.getString("sampleDirectory"))) {
			throw new BadRequestException("sampleDirectory cannot be changed when resuming a job");
		}
		if (request.chunkSize() != null) {
			if (request.chunkSize() <= 0) {
				throw new BadRequestException("chunkSize must be greater than zero");
			}
			builder.addLong("chunkSize", request.chunkSize().longValue(), false);
		}
		if (request.writerStrategy() != null && !request.writerStrategy().isBlank()) {
			builder.addString("writerStrategy", writerStrategy(request.writerStrategy()).name(), false);
		}
		builder.addLocalDateTime("resumedAt", LocalDateTime.now(), false);
		return builder.toJobParameters();
	}

	private JobParameters fallbackResumeParameters(EtlJobEntity job, ImportJobRequest request) {
		String sampleDirectory = job.getSampleDirectory();
		if (request != null && request.sampleDirectory() != null && !request.sampleDirectory().isBlank()
				&& !request.sampleDirectory().equals(sampleDirectory)) {
			throw new BadRequestException("sampleDirectory cannot be changed when resuming a job");
		}
		int chunkSize = request != null && request.chunkSize() != null
				? request.chunkSize()
				: properties.batch().defaultChunkSize();
		if (chunkSize <= 0) {
			throw new BadRequestException("chunkSize must be greater than zero");
		}
		StagingWriterStrategy writerStrategy = writerStrategy(request == null ? null : request.writerStrategy());
		return new JobParametersBuilder()
				.addString("jobId", job.getJobId(), true)
				.addString("sampleDirectory", sampleDirectory, true)
				.addString("writerStrategy", writerStrategy.name(), false)
				.addLong("chunkSize", (long) chunkSize, false)
				.addLocalDateTime("fallbackResumedAt", LocalDateTime.now(), false)
				.toJobParameters();
	}

	private Optional<JobExecution> latestBatchExecution(String jobId) {
		List<JobExecution> executions = new ArrayList<>();
		for (int start = 0; ; start += JOB_INSTANCE_PAGE_SIZE) {
			List<JobInstance> instances = batchJobRepository.getJobInstances(ETL_JOB_NAME, start, JOB_INSTANCE_PAGE_SIZE);
			if (instances.isEmpty()) {
				break;
			}
			for (JobInstance instance : instances) {
				executions.addAll(batchJobRepository.getJobExecutions(instance).stream()
						.filter(execution -> jobId.equals(execution.getJobParameters().getString("jobId")))
						.toList());
			}
		}
		return executions.stream()
				.max(Comparator
						.comparing(JobExecution::getCreateTime, Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparingLong(JobExecution::getId));
	}

	private JobExecution recoverIfStale(JobExecution jobExecution) {
		if (!jobExecution.getStatus().isRunning()) {
			return jobExecution;
		}
		LocalDateTime now = LocalDateTime.now();
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			if (stepExecution.getStatus().isRunning()) {
				stepExecution.setStatus(BatchStatus.FAILED);
				stepExecution.setExitStatus(new ExitStatus(ExitStatus.FAILED.getExitCode(),
						"Recovered stale running step before resume"));
				stepExecution.setEndTime(now);
				stepExecution.setLastUpdated(now);
				batchJobRepository.update(stepExecution);
			}
		}
		jobExecution.setStatus(BatchStatus.FAILED);
		jobExecution.setExitStatus(new ExitStatus(ExitStatus.FAILED.getExitCode(),
				"Recovered stale running job before resume"));
		jobExecution.setEndTime(now);
		jobExecution.setLastUpdated(now);
		batchJobRepository.update(jobExecution);
		LOGGER.info("etl_job_recovered_stale_execution jobExecutionId={} status=FAILED", jobExecution.getId());
		return jobExecution;
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
