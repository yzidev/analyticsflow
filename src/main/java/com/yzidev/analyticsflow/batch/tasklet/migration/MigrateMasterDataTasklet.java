package com.yzidev.analyticsflow.batch.tasklet.migration;

import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.yzidev.analyticsflow.batch.migration.MigrationResult;
import com.yzidev.analyticsflow.batch.migration.StagingMigrationService;

@Component
public class MigrateMasterDataTasklet implements Tasklet {

	private final StagingMigrationService stagingMigrationService;

	public MigrateMasterDataTasklet(StagingMigrationService stagingMigrationService) {
		this.stagingMigrationService = stagingMigrationService;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		MigrationResult result = stagingMigrationService.migrateMasterData(jobId(chunkContext));
		updateCounts(contribution, result);
		return RepeatStatus.FINISHED;
	}

	private String jobId(ChunkContext chunkContext) {
		return chunkContext.getStepContext().getJobParameters().get("jobId").toString();
	}

	private void updateCounts(StepContribution contribution, MigrationResult result) {
		contribution.getStepExecution().setReadCount(result.readCount());
		contribution.getStepExecution().setWriteCount(result.writeCount());
		contribution.getStepExecution().setProcessSkipCount(result.invalidCount());
	}
}
