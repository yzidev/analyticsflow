package com.yzidev.analyticsflow.reporting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.yzidev.analyticsflow.common.enums.ReportStrategy;
import com.yzidev.analyticsflow.dto.response.ReportSlice;
import com.yzidev.analyticsflow.repository.reactive.ReactiveAnalyticsReportRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class ReactiveReportServiceTest {

	@Test
	void benchmarkZipsReactiveQueriesAndMergesResults() {
		ReactiveAnalyticsReportRepository repository = mock(ReactiveAnalyticsReportRepository.class);
		when(repository.salesDailySummary()).thenReturn(Mono.just(slice(1, 10, "WEB")));
		when(repository.salesProductSummary()).thenReturn(Mono.just(slice(2, 20, null)));
		when(repository.salesCustomerSummary()).thenReturn(Mono.just(slice(3, 30, null)));
		when(repository.deliveryPerformanceSummary()).thenReturn(Mono.just(slice(4, 40, null)));
		when(repository.paymentMethodSummary()).thenReturn(Mono.just(slice(5, 50, null)));
		when(repository.channelSalesSummary()).thenReturn(Mono.just(slice(6, 60, "MOBILE")));

		var service = new ReactiveReportService(repository, new ReportAggregationSupport(null));

		StepVerifier.create(service.benchmark())
				.assertNext(response -> {
					assertThat(response.strategy()).isEqualTo(ReportStrategy.REACTIVE);
					assertThat(response.totalOrders()).isEqualTo(21);
					assertThat(response.totalItemsSold()).isEqualTo(210);
					assertThat(response.totalRevenue()).isEqualByComparingTo("2100");
					assertThat(response.topChannel()).isEqualTo("MOBILE");
					assertThat(response.threadName()).isNotBlank();
				})
				.verifyComplete();
	}

	@Test
	void benchmarkMapsReactiveFailures() {
		ReactiveAnalyticsReportRepository repository = mock(ReactiveAnalyticsReportRepository.class);
		when(repository.salesDailySummary()).thenReturn(Mono.error(new IllegalStateException("r2dbc query failed")));
		when(repository.salesProductSummary()).thenReturn(Mono.just(slice(1, 1, null)));
		when(repository.salesCustomerSummary()).thenReturn(Mono.just(slice(1, 1, null)));
		when(repository.deliveryPerformanceSummary()).thenReturn(Mono.just(slice(1, 1, null)));
		when(repository.paymentMethodSummary()).thenReturn(Mono.just(slice(1, 1, null)));
		when(repository.channelSalesSummary()).thenReturn(Mono.just(slice(1, 1, null)));

		var service = new ReactiveReportService(repository, new ReportAggregationSupport(null));

		StepVerifier.create(service.benchmark())
				.expectErrorMatches(error -> error instanceof IllegalStateException
						&& error.getMessage().contains("r2dbc query failed"))
				.verify();
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
