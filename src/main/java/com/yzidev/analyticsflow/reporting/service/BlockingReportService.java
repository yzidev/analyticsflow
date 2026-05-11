package com.yzidev.analyticsflow.reporting.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.yzidev.analyticsflow.common.enums.ReportStrategy;
import com.yzidev.analyticsflow.dto.response.ReportBenchmarkResponse;
import com.yzidev.analyticsflow.repository.blocking.BlockingAnalyticsReportRepository;

@Service
public class BlockingReportService {

	private final BlockingAnalyticsReportRepository repository;
	private final ReportAggregationSupport aggregationSupport;

	public BlockingReportService(BlockingAnalyticsReportRepository repository, ReportAggregationSupport aggregationSupport) {
		this.repository = repository;
		this.aggregationSupport = aggregationSupport;
	}

	public ReportBenchmarkResponse benchmark() {
		Instant startedAt = Instant.now();
		var merged = aggregationSupport.merge(List.of(
				repository.salesDailySummary(),
				repository.salesProductSummary(),
				repository.salesCustomerSummary(),
				repository.deliveryPerformanceSummary(),
				repository.paymentMethodSummary(),
				repository.channelSalesSummary()));
		return aggregationSupport.response(ReportStrategy.BLOCKING, startedAt, merged, Thread.currentThread().getName());
	}
}
