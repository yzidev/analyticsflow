package com.yzidev.analyticsflow.batch.listener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yzidev.analyticsflow.common.enums.EtlJobStatus;
import com.yzidev.analyticsflow.entity.support.EtlJobEntity;
import com.yzidev.analyticsflow.observability.AnalyticsFlowMetrics;
import com.yzidev.analyticsflow.repository.jpa.support.EtlJobRepository;

@Component
public class JobCompletionListener implements JobExecutionListener {

	private final EtlJobRepository etlJobRepository;
	private final AnalyticsFlowMetrics metrics;

	public JobCompletionListener(EtlJobRepository etlJobRepository, AnalyticsFlowMetrics metrics) {
		this.etlJobRepository = etlJobRepository;
		this.metrics = metrics;
	}

	@Override
	@Transactional
	public void beforeJob(JobExecution jobExecution) {
		String jobId = jobId(jobExecution);
		EtlJobEntity job = etlJobRepository.findByJobId(jobId).orElseGet(EtlJobEntity::new);
		job.setJobId(jobId);
		job.setSampleDirectory(jobExecution.getJobParameters().getString("sampleDirectory", "data/files"));
		job.setStatus(EtlJobStatus.RUNNING);
		job.setStartedAt(jobExecution.getStartTime());
		job.setUpdatedAt(LocalDateTime.now());
		etlJobRepository.save(job);
		metrics.jobFinished(job.getStatus(), value(job.getProcessedRows()), job.getDurationMs());
	}

	@Override
	@Transactional
	public void afterJob(JobExecution jobExecution) {
		String jobId = jobId(jobExecution);
		EtlJobEntity job = etlJobRepository.findByJobId(jobId).orElseGet(EtlJobEntity::new);
		job.setJobId(jobId);
		job.setSampleDirectory(jobExecution.getJobParameters().getString("sampleDirectory", "data/files"));
		job.setStatus(BatchStatusMapper.toEtlStatus(jobExecution.getStatus()));
		job.setTotalRows(totalReadCount(jobExecution));
		job.setProcessedRows(totalReadCount(jobExecution));
		job.setSuccessRows(totalWriteCount(jobExecution));
		job.setFailedRows(totalSkipCount(jobExecution));
		job.setStartedAt(jobExecution.getStartTime());
		job.setFinishedAt(jobExecution.getEndTime());
		job.setDurationMs(durationMs(jobExecution.getStartTime(), jobExecution.getEndTime()));
		job.setErrorMessage(errorMessage(jobExecution));
		job.setUpdatedAt(LocalDateTime.now());
		etlJobRepository.save(job);
	}

	private String jobId(JobExecution jobExecution) {
		return jobExecution.getJobParameters().getString("jobId");
	}

	private long totalReadCount(JobExecution jobExecution) {
		return jobExecution.getStepExecutions().stream().mapToLong(step -> step.getReadCount()).sum();
	}

	private long totalWriteCount(JobExecution jobExecution) {
		return jobExecution.getStepExecutions().stream().mapToLong(step -> step.getWriteCount()).sum();
	}

	private long totalSkipCount(JobExecution jobExecution) {
		return jobExecution.getStepExecutions().stream().mapToLong(step -> step.getSkipCount()).sum();
	}

	private long value(Long value) {
		return value == null ? 0 : value;
	}

	private Long durationMs(LocalDateTime start, LocalDateTime end) {
		if (start == null || end == null) {
			return null;
		}
		return Duration.between(start, end).toMillis();
	}

	private String errorMessage(JobExecution jobExecution) {
		if (jobExecution.getAllFailureExceptions().isEmpty()) {
			return null;
		}
		return jobExecution.getAllFailureExceptions().stream()
				.map(Throwable::getMessage)
				.collect(Collectors.joining("; "));
	}
}
