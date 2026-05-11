package com.yzidev.analyticsflow.batch.tasklet;

import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.yzidev.analyticsflow.dto.response.FileValidationResponse;
import com.yzidev.analyticsflow.ingestion.validator.CsvFileValidator;

@Component
public class ValidateRequiredFilesTasklet implements Tasklet {

	private final CsvFileValidator csvFileValidator;

	public ValidateRequiredFilesTasklet(CsvFileValidator csvFileValidator) {
		this.csvFileValidator = csvFileValidator;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		String sampleDirectory = (String) chunkContext.getStepContext().getJobParameters().get("sampleDirectory");
		FileValidationResponse response = csvFileValidator.validate(sampleDirectory);
		if (!response.valid()) {
			throw new IllegalStateException("CSV validation failed. Missing files: " + response.missingFiles()
					+ ", invalid headers: " + response.invalidHeaders() + ", errors: " + response.errors());
		}
		response.files().forEach(file -> contribution.incrementReadCount());
		contribution.incrementWriteCount(response.files().size());
		return RepeatStatus.FINISHED;
	}
}
