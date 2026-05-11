package com.yzidev.analyticsflow.batch.tasklet.report;

import java.util.List;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import com.yzidev.analyticsflow.dto.response.ReportMetadataResponse;
import com.yzidev.analyticsflow.reporting.service.ReportExportService;

@Component
public class GenerateInitialReportTasklet implements Tasklet {

	private final ReportExportService reportExportService;

	public GenerateInitialReportTasklet(ReportExportService reportExportService) {
		this.reportExportService = reportExportService;
	}

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
		List<ReportMetadataResponse> reports = reportExportService.generateInitialReports(jobId(chunkContext));
		contribution.getStepExecution().setReadCount(reports.size());
		contribution.getStepExecution().setWriteCount(reports.size());
		return RepeatStatus.FINISHED;
	}

	private String jobId(ChunkContext chunkContext) {
		return chunkContext.getStepContext().getJobParameters().get("jobId").toString();
	}
}
