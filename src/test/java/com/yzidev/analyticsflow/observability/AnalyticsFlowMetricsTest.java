package com.yzidev.analyticsflow.observability;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.yzidev.analyticsflow.common.enums.EtlJobStatus;
import com.yzidev.analyticsflow.common.enums.ReportGenerationStatus;
import com.yzidev.analyticsflow.common.enums.ReportType;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class AnalyticsFlowMetricsTest {

	@Test
	void recordsJobStepAndReportMetrics() {
		SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
		AnalyticsFlowMetrics metrics = new AnalyticsFlowMetrics(meterRegistry);

		metrics.jobStarted();
		metrics.jobFinished(EtlJobStatus.COMPLETED, 12, 300L);
		metrics.stepFinished("IMPORT_USERS_TO_STAGING", EtlJobStatus.COMPLETED, 12, 10, 2, 120L);
		metrics.reportGenerated(ReportType.SALES_DAILY_SUMMARY, ReportGenerationStatus.GENERATED, 75L);

		assertThat(meterRegistry.counter("analyticsflow.etl.jobs.total", "status", "started").count())
				.isEqualTo(1.0);
		assertThat(meterRegistry.counter("analyticsflow.etl.jobs.completed").count()).isEqualTo(1.0);
		assertThat(meterRegistry.counter("analyticsflow.etl.rows.processed").count()).isEqualTo(12.0);
		assertThat(meterRegistry.counter("analyticsflow.etl.step.rows.skipped", "step", "IMPORT_USERS_TO_STAGING").count())
				.isEqualTo(2.0);
		assertThat(meterRegistry.counter("analyticsflow.reports.generated",
				"type", "SALES_DAILY_SUMMARY", "status", "generated").count()).isEqualTo(1.0);
		assertThat(meterRegistry.timer("analyticsflow.report.generation.duration", "type", "SALES_DAILY_SUMMARY")
				.count()).isEqualTo(1);
	}
}
