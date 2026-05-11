package com.yzidev.analyticsflow.batch.tasklet.transform;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.yzidev.analyticsflow.batch.transform.AnalyticalTransformationService;
import com.yzidev.analyticsflow.batch.transform.TransformationResult;

@Component
public class TransformToAnalyticalTablesTasklet implements Tasklet {

	private final AnalyticalTransformationService analyticalTransformationService;

	public TransformToAnalyticalTablesTasklet(AnalyticalTransformationService analyticalTransformationService) {
		this.analyticalTransformationService = analyticalTransformationService;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		TransformationResult result = analyticalTransformationService.transform();
		contribution.getStepExecution().setReadCount(result.readCount());
		contribution.getStepExecution().setWriteCount(result.writeCount());
		return RepeatStatus.FINISHED;
	}
}
