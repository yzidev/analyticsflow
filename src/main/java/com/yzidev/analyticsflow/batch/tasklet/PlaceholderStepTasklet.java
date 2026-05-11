package com.yzidev.analyticsflow.batch.tasklet;

import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.yzidev.analyticsflow.common.enums.EtlStepName;

@Component
public class PlaceholderStepTasklet {

	public Tasklet forStep(EtlStepName stepName) {
		return (StepContribution contribution, ChunkContext chunkContext) -> {
			contribution.incrementReadCount();
			contribution.incrementWriteCount(1);
			chunkContext.getStepContext().getStepExecution().getExecutionContext()
					.putString("phase5.note", stepName.name() + " is wired; implementation continues in the next phase.");
			return RepeatStatus.FINISHED;
		};
	}
}
