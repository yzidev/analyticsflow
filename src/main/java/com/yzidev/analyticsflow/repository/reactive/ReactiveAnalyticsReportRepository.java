package com.yzidev.analyticsflow.repository.reactive;

import com.yzidev.analyticsflow.dto.response.ReportSlice;

import reactor.core.publisher.Mono;

public interface ReactiveAnalyticsReportRepository {

	Mono<ReportSlice> salesDailySummary();

	Mono<ReportSlice> salesProductSummary();

	Mono<ReportSlice> salesCustomerSummary();

	Mono<ReportSlice> deliveryPerformanceSummary();

	Mono<ReportSlice> paymentMethodSummary();

	Mono<ReportSlice> channelSalesSummary();
}
