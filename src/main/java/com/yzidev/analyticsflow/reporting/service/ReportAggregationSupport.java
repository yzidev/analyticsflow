package com.yzidev.analyticsflow.reporting.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Component;

import com.yzidev.analyticsflow.common.enums.ReportStrategy;
import com.yzidev.analyticsflow.config.AnalyticsFlowProperties;
import com.yzidev.analyticsflow.dto.response.ReportBenchmarkResponse;
import com.yzidev.analyticsflow.dto.response.ReportSlice;

@Component
public class ReportAggregationSupport {

	private final AnalyticsFlowProperties properties;

	public ReportAggregationSupport(AnalyticsFlowProperties properties) {
		this.properties = properties;
	}

	public ReportSlice merge(List<ReportSlice> slices) {
		return slices.stream().reduce(ReportSlice.empty(), ReportSlice::merge);
	}

	public ReportBenchmarkResponse response(ReportStrategy strategy, Instant startedAt, ReportSlice merged, String threadName) {
		return new ReportBenchmarkResponse(
				strategy,
				merged.totalOrders(),
				merged.totalItemsSold(),
				merged.totalRevenue(),
				merged.totalCustomers(),
				merged.totalProducts(),
				merged.totalDeliveredOrders(),
				merged.totalSuccessfulTransactions(),
				merged.topChannel(),
				Duration.between(startedAt, Instant.now()).toMillis(),
				threadName,
				"postgresql");
	}
}
