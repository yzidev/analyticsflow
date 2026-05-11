package com.yzidev.analyticsflow.reporting.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Service;

import com.yzidev.analyticsflow.common.enums.ReportStrategy;
import com.yzidev.analyticsflow.dto.response.ReportBenchmarkResponse;
import com.yzidev.analyticsflow.dto.response.ReportSlice;
import com.yzidev.analyticsflow.repository.reactive.ReactiveAnalyticsReportRepository;

import reactor.core.publisher.Mono;

@Service
public class ReactiveReportService {

	private static final Duration QUERY_TIMEOUT = Duration.ofSeconds(30);

	private final ReactiveAnalyticsReportRepository repository;
	private final ReportAggregationSupport aggregationSupport;

	public ReactiveReportService(ReactiveAnalyticsReportRepository repository, ReportAggregationSupport aggregationSupport) {
		this.repository = repository;
		this.aggregationSupport = aggregationSupport;
	}

	public Mono<ReportBenchmarkResponse> benchmark() {
		Instant startedAt = Instant.now();
		return Mono.zip(
				query(repository.salesDailySummary()),
				query(repository.salesProductSummary()),
				query(repository.salesCustomerSummary()),
				query(repository.deliveryPerformanceSummary()),
				query(repository.paymentMethodSummary()),
				query(repository.channelSalesSummary()))
				.map(tuple -> {
					List<ReportQueryResult> results = List.of(
							tuple.getT1(),
							tuple.getT2(),
							tuple.getT3(),
							tuple.getT4(),
							tuple.getT5(),
							tuple.getT6());
					ReportSlice merged = aggregationSupport.merge(results.stream().map(ReportQueryResult::slice).toList());
					return aggregationSupport.response(ReportStrategy.REACTIVE, startedAt, merged, threadNames(results));
				})
				.timeout(QUERY_TIMEOUT)
				.onErrorMap(TimeoutException.class, exception -> new IllegalStateException(
						"Reactive report benchmark timed out after %d seconds".formatted(QUERY_TIMEOUT.toSeconds()),
						exception))
				.onErrorMap(exception -> !(exception instanceof IllegalStateException),
						exception -> new IllegalStateException(
								"Reactive report benchmark failed: " + exception.getMessage(),
								exception));
	}

	private Mono<ReportQueryResult> query(Mono<ReportSlice> query) {
		return query
				.map(slice -> new ReportQueryResult(slice, Thread.currentThread().getName()))
				.switchIfEmpty(Mono.defer(() -> Mono.just(new ReportQueryResult(
						ReportSlice.empty(),
						Thread.currentThread().getName()))));
	}

	private String threadNames(List<ReportQueryResult> results) {
		return results.stream()
				.map(ReportQueryResult::threadName)
				.distinct()
				.limit(6)
				.collect(java.util.stream.Collectors.joining(", "));
	}
}
