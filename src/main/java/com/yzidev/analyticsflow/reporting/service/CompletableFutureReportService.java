package com.yzidev.analyticsflow.reporting.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.yzidev.analyticsflow.common.enums.ReportStrategy;
import com.yzidev.analyticsflow.dto.response.ReportBenchmarkResponse;
import com.yzidev.analyticsflow.dto.response.ReportSlice;
import com.yzidev.analyticsflow.repository.blocking.BlockingAnalyticsReportRepository;

@Service
public class CompletableFutureReportService {

	private static final long QUERY_TIMEOUT_SECONDS = 30;

	private final BlockingAnalyticsReportRepository repository;
	private final ReportAggregationSupport aggregationSupport;
	private final Executor executor;

	public CompletableFutureReportService(
			BlockingAnalyticsReportRepository repository,
			ReportAggregationSupport aggregationSupport,
			@Qualifier("analyticsCompletableFutureExecutor") Executor executor) {
		this.repository = repository;
		this.aggregationSupport = aggregationSupport;
		this.executor = executor;
	}

	public ReportBenchmarkResponse benchmark() {
		Instant startedAt = Instant.now();
		List<CompletableFuture<ReportQueryResult>> futures = List.of(
				query(repository::salesDailySummary),
				query(repository::salesProductSummary),
				query(repository::salesCustomerSummary),
				query(repository::deliveryPerformanceSummary),
				query(repository::paymentMethodSummary),
				query(repository::channelSalesSummary));

		try {
			CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
					.orTimeout(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
					.join();
			var slices = futures.stream().map(CompletableFuture::join).toList();
			return aggregationSupport.response(ReportStrategy.COMPLETABLE_FUTURE, startedAt,
					aggregationSupport.merge(slices.stream().map(ReportQueryResult::slice).toList()),
					threadNames(slices));
		}
		catch (CompletionException exception) {
			futures.forEach(future -> future.cancel(true));
			throw benchmarkFailure(exception);
		}
	}

	private CompletableFuture<ReportQueryResult> query(java.util.concurrent.Callable<ReportSlice> query) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				return new ReportQueryResult(query.call(), Thread.currentThread().getName());
			}
			catch (Exception exception) {
				throw new CompletionException(exception);
			}
		}, executor).orTimeout(QUERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
	}

	private RuntimeException benchmarkFailure(CompletionException exception) {
		Throwable cause = unwrap(exception);
		if (cause instanceof TimeoutException) {
			return new IllegalStateException(
					"CompletableFuture report benchmark timed out after %d seconds".formatted(QUERY_TIMEOUT_SECONDS),
					cause);
		}
		return new IllegalStateException("CompletableFuture report benchmark failed: " + cause.getMessage(), cause);
	}

	private Throwable unwrap(Throwable exception) {
		Throwable current = exception;
		while (current instanceof CompletionException && current.getCause() != null) {
			current = current.getCause();
		}
		return current;
	}

	private String threadNames(List<ReportQueryResult> results) {
		return results.stream()
				.map(ReportQueryResult::threadName)
				.distinct()
				.limit(6)
				.collect(java.util.stream.Collectors.joining(", "));
	}
}
