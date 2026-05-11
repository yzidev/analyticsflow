package com.yzidev.analyticsflow.reporting.service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.yzidev.analyticsflow.common.enums.ReportStrategy;
import com.yzidev.analyticsflow.dto.response.ReportBenchmarkResponse;
import com.yzidev.analyticsflow.dto.response.ReportSlice;
import com.yzidev.analyticsflow.repository.blocking.BlockingAnalyticsReportRepository;

@Service
public class VirtualThreadReportService {

	private final BlockingAnalyticsReportRepository repository;
	private final ReportAggregationSupport aggregationSupport;
	private final ExecutorService virtualThreadExecutor;

	public VirtualThreadReportService(
			BlockingAnalyticsReportRepository repository,
			ReportAggregationSupport aggregationSupport,
			@Qualifier("analyticsVirtualThreadExecutor") ExecutorService virtualThreadExecutor) {
		this.repository = repository;
		this.aggregationSupport = aggregationSupport;
		this.virtualThreadExecutor = virtualThreadExecutor;
	}

	public ReportBenchmarkResponse benchmark() {
		Instant startedAt = Instant.now();
		try {
			List<Callable<ReportQueryResult>> tasks = List.of(
					query(repository::salesDailySummary),
					query(repository::salesProductSummary),
					query(repository::salesCustomerSummary),
					query(repository::deliveryPerformanceSummary),
					query(repository::paymentMethodSummary),
					query(repository::channelSalesSummary));
			var slices = virtualThreadExecutor.invokeAll(tasks).stream()
					.map(future -> {
						try {
							return future.get();
						}
						catch (InterruptedException exception) {
							Thread.currentThread().interrupt();
							throw new IllegalStateException(exception);
						}
						catch (ExecutionException exception) {
							throw new IllegalStateException(exception.getCause());
						}
					})
					.toList();
			return aggregationSupport.response(ReportStrategy.VIRTUAL_THREAD, startedAt,
					aggregationSupport.merge(slices.stream().map(ReportQueryResult::slice).toList()),
					threadNames(slices));
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(exception);
		}
	}

	private Callable<ReportQueryResult> query(Callable<ReportSlice> query) {
		return () -> {
			Thread thread = Thread.currentThread();
			return new ReportQueryResult(query.call(), thread + " virtual=" + thread.isVirtual());
		};
	}

	private String threadNames(List<ReportQueryResult> results) {
		return results.stream()
				.map(ReportQueryResult::threadName)
				.distinct()
				.limit(6)
				.collect(java.util.stream.Collectors.joining(", "));
	}
}
