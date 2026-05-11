package com.yzidev.analyticsflow.observability;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.yzidev.analyticsflow.common.enums.EtlJobStatus;
import com.yzidev.analyticsflow.common.enums.ReportGenerationStatus;
import com.yzidev.analyticsflow.common.enums.ReportType;

import io.micrometer.core.instrument.MeterRegistry;

@Component
public class AnalyticsFlowMetrics {

	private final MeterRegistry meterRegistry;

	public AnalyticsFlowMetrics(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	public void jobStarted() {
		meterRegistry.counter("analyticsflow.etl.jobs.total", "status", "started").increment();
	}

	public void jobFinished(EtlJobStatus status, long processedRows, Long durationMs) {
		meterRegistry.counter("analyticsflow.etl.jobs.total", "status", status.name().toLowerCase()).increment();
		if (status == EtlJobStatus.COMPLETED) {
			meterRegistry.counter("analyticsflow.etl.jobs.completed").increment();
		}
		if (status == EtlJobStatus.FAILED) {
			meterRegistry.counter("analyticsflow.etl.jobs.failed").increment();
		}
		if (processedRows > 0) {
			meterRegistry.counter("analyticsflow.etl.rows.processed").increment(processedRows);
		}
		recordDuration("analyticsflow.etl.job.duration", durationMs);
	}

	public void stepFinished(String stepName, EtlJobStatus status, long readCount, long writeCount, long skipCount,
			Long durationMs) {
		increment("analyticsflow.etl.step.rows.read", readCount, "step", stepName);
		increment("analyticsflow.etl.step.rows.written", writeCount, "step", stepName);
		increment("analyticsflow.etl.step.rows.skipped", skipCount, "step", stepName);
		meterRegistry.counter("analyticsflow.etl.steps.total", "step", stepName, "status", status.name().toLowerCase())
				.increment();
		recordDuration("analyticsflow.etl.step.duration", durationMs, "step", stepName);
	}

	public void reportGenerated(ReportType reportType, ReportGenerationStatus status, Long durationMs) {
		meterRegistry.counter("analyticsflow.reports.generated", "type", reportType.name(),
				"status", status.name().toLowerCase()).increment();
		recordDuration("analyticsflow.report.generation.duration", durationMs, "type", reportType.name());
	}

	private void increment(String metricName, long amount, String... tags) {
		var counter = meterRegistry.counter(metricName, tags);
		if (amount > 0) {
			counter.increment(amount);
		}
	}

	private void recordDuration(String metricName, Long durationMs, String... tags) {
		if (durationMs == null || durationMs < 0) {
			return;
		}
		meterRegistry.timer(metricName, tags).record(durationMs, TimeUnit.MILLISECONDS);
	}
}
