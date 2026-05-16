package com.yzidev.analyticsflow.batch.listener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yzidev.analyticsflow.common.enums.EtlJobStatus;
import com.yzidev.analyticsflow.common.enums.EtlStepName;
import com.yzidev.analyticsflow.entity.support.EtlJobStepEntity;
import com.yzidev.analyticsflow.observability.AnalyticsFlowMetrics;
import com.yzidev.analyticsflow.repository.jpa.support.EtlJobStepRepository;

@Component
public class StepProgressListener implements StepExecutionListener {

	private final EtlJobStepRepository etlJobStepRepository;
	private final AnalyticsFlowMetrics metrics;

	public StepProgressListener(EtlJobStepRepository etlJobStepRepository, AnalyticsFlowMetrics metrics) {
		this.etlJobStepRepository = etlJobStepRepository;
		this.metrics = metrics;
	}

	@Override
	@Transactional(transactionManager = "transactionManager")
	public void beforeStep(StepExecution stepExecution) {
		String jobId = jobId(stepExecution);
		EtlStepName stepName = EtlStepName.valueOf(stepExecution.getStepName());
		EtlJobStepEntity step = etlJobStepRepository.findByJobIdAndStepName(jobId, stepName)
				.orElseGet(EtlJobStepEntity::new);
		step.setJobId(jobId);
		step.setStepName(stepName);
		step.setStatus(EtlJobStatus.RUNNING);
		step.setStartedAt(stepExecution.getStartTime());
		step.setUpdatedAt(LocalDateTime.now());
		etlJobStepRepository.save(step);
	}

	@Override
	@Transactional(transactionManager = "transactionManager")
	public ExitStatus afterStep(StepExecution stepExecution) {
		String jobId = jobId(stepExecution);
		EtlStepName stepName = EtlStepName.valueOf(stepExecution.getStepName());
		EtlJobStepEntity step = etlJobStepRepository.findByJobIdAndStepName(jobId, stepName)
				.orElseGet(EtlJobStepEntity::new);
		step.setJobId(jobId);
		step.setStepName(stepName);
		step.setStatus(BatchStatusMapper.toEtlStatus(stepExecution.getStatus()));
		step.setReadCount(stepExecution.getReadCount());
		step.setWriteCount(stepExecution.getWriteCount());
		step.setSkipCount(stepExecution.getSkipCount());
		step.setStartedAt(stepExecution.getStartTime());
		step.setFinishedAt(stepExecution.getEndTime());
		step.setDurationMs(durationMs(stepExecution.getStartTime(), stepExecution.getEndTime()));
		step.setErrorMessage(errorMessage(stepExecution));
		step.setUpdatedAt(LocalDateTime.now());
		etlJobStepRepository.save(step);
		metrics.stepFinished(stepName.name(), step.getStatus(), stepExecution.getReadCount(),
				stepExecution.getWriteCount(), stepExecution.getSkipCount(), step.getDurationMs());
		return stepExecution.getExitStatus();
	}

	private String jobId(StepExecution stepExecution) {
		return stepExecution.getJobParameters().getString("jobId");
	}

	private Long durationMs(LocalDateTime start, LocalDateTime end) {
		if (start == null || end == null) {
			return null;
		}
		return Duration.between(start, end).toMillis();
	}

	private String errorMessage(StepExecution stepExecution) {
		if (stepExecution.getFailureExceptions().isEmpty()) {
			return null;
		}
		return stepExecution.getFailureExceptions().stream()
				.map(Throwable::getMessage)
				.collect(Collectors.joining("; "));
	}
}
