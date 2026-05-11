package com.yzidev.analyticsflow.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.yzidev.analyticsflow.common.enums.ReportStrategy;
import com.yzidev.analyticsflow.dto.response.ReportSlice;
import com.yzidev.analyticsflow.repository.blocking.BlockingAnalyticsReportRepository;

class CompletableFutureReportServiceTest {

	private final ExecutorService executor = Executors.newFixedThreadPool(6, runnable -> {
		Thread thread = new Thread(runnable);
		thread.setName("analyticsflow-cf-test-" + COUNTER.incrementAndGet());
		return thread;
	});

	private static final AtomicInteger COUNTER = new AtomicInteger();

	@AfterEach
	void tearDown() {
		executor.shutdownNow();
	}

	@Test
	void benchmarkRunsQueriesInParallelAndMergesResults() {
		BlockingAnalyticsReportRepository repository = mock(BlockingAnalyticsReportRepository.class);
		when(repository.salesDailySummary()).thenReturn(slice(1, 10, "WEB"));
		when(repository.salesProductSummary()).thenReturn(slice(2, 20, null));
		when(repository.salesCustomerSummary()).thenReturn(slice(3, 30, null));
		when(repository.deliveryPerformanceSummary()).thenReturn(slice(4, 40, null));
		when(repository.paymentMethodSummary()).thenReturn(slice(5, 50, null));
		when(repository.channelSalesSummary()).thenReturn(slice(6, 60, "MOBILE"));

		var service = new CompletableFutureReportService(
				repository,
				new ReportAggregationSupport(null),
				executor);

		var response = service.benchmark();

		assertThat(response.strategy()).isEqualTo(ReportStrategy.COMPLETABLE_FUTURE);
		assertThat(response.totalOrders()).isEqualTo(21);
		assertThat(response.totalItemsSold()).isEqualTo(210);
		assertThat(response.totalRevenue()).isEqualByComparingTo("2100");
		assertThat(response.topChannel()).isEqualTo("MOBILE");
		assertThat(response.threadName()).contains("analyticsflow-cf-test-");
	}

	@Test
	void benchmarkPreservesQueryFailureMessage() {
		BlockingAnalyticsReportRepository repository = mock(BlockingAnalyticsReportRepository.class);
		when(repository.salesDailySummary()).thenThrow(new IllegalStateException("database query failed"));
		when(repository.salesProductSummary()).thenReturn(slice(1, 1, null));
		when(repository.salesCustomerSummary()).thenReturn(slice(1, 1, null));
		when(repository.deliveryPerformanceSummary()).thenReturn(slice(1, 1, null));
		when(repository.paymentMethodSummary()).thenReturn(slice(1, 1, null));
		when(repository.channelSalesSummary()).thenReturn(slice(1, 1, null));

		var service = new CompletableFutureReportService(
				repository,
				new ReportAggregationSupport(null),
				executor);

		assertThatThrownBy(service::benchmark)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("CompletableFuture report benchmark failed")
				.hasRootCauseMessage("database query failed");
	}

	private ReportSlice slice(long orders, long itemsSold, String topChannel) {
		return new ReportSlice(
				orders,
				itemsSold,
				BigDecimal.valueOf(itemsSold * 10),
				orders,
				orders,
				orders,
				orders,
				topChannel);
	}
}
