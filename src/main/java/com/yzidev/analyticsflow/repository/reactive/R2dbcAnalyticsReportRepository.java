package com.yzidev.analyticsflow.repository.reactive;

import java.math.BigDecimal;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import com.yzidev.analyticsflow.dto.response.ReportSlice;

import io.r2dbc.spi.Row;
import reactor.core.publisher.Mono;

@Repository
public class R2dbcAnalyticsReportRepository implements ReactiveAnalyticsReportRepository {

	private final DatabaseClient databaseClient;

	public R2dbcAnalyticsReportRepository(DatabaseClient databaseClient) {
		this.databaseClient = databaseClient;
	}

	@Override
	public Mono<ReportSlice> salesDailySummary() {
		return databaseClient.sql("""
				SELECT
					COALESCE(SUM(total_orders), 0) AS total_orders,
					COALESCE(SUM(total_items_sold), 0) AS total_items_sold,
					COALESCE(SUM(total_gross_revenue), 0) AS total_revenue,
					COALESCE(SUM(total_delivered_orders), 0) AS total_delivered_orders,
					COALESCE(SUM(total_success_transactions), 0) AS total_successful_transactions
				FROM analyticsflow_olap.sales_daily_summary
				""")
				.map((row, metadata) -> new ReportSlice(
						longValue(row, "total_orders"),
						longValue(row, "total_items_sold"),
						bigDecimalValue(row, "total_revenue"),
						0,
						0,
						longValue(row, "total_delivered_orders"),
						longValue(row, "total_successful_transactions"),
						null))
				.one();
	}

	@Override
	public Mono<ReportSlice> salesProductSummary() {
		return databaseClient.sql("""
				SELECT COALESCE(COUNT(DISTINCT product_id), 0) AS total_products
				FROM analyticsflow_olap.sales_product_summary
				""")
				.map((row, metadata) -> new ReportSlice(0, 0, BigDecimal.ZERO, 0,
						longValue(row, "total_products"), 0, 0, null))
				.one();
	}

	@Override
	public Mono<ReportSlice> salesCustomerSummary() {
		return databaseClient.sql("""
				SELECT COALESCE(COUNT(DISTINCT user_id), 0) AS total_customers
				FROM analyticsflow_olap.sales_customer_summary
				""")
				.map((row, metadata) -> new ReportSlice(0, 0, BigDecimal.ZERO,
						longValue(row, "total_customers"), 0, 0, 0, null))
				.one();
	}

	@Override
	public Mono<ReportSlice> deliveryPerformanceSummary() {
		return databaseClient.sql("""
				SELECT COALESCE(SUM(total_delivered), 0) AS total_delivered_orders
				FROM analyticsflow_olap.delivery_performance_summary
				""")
				.map((row, metadata) -> new ReportSlice(0, 0, BigDecimal.ZERO, 0, 0,
						longValue(row, "total_delivered_orders"), 0, null))
				.one();
	}

	@Override
	public Mono<ReportSlice> paymentMethodSummary() {
		return databaseClient.sql("""
				SELECT COALESCE(SUM(total_success), 0) AS total_successful_transactions
				FROM analyticsflow_olap.payment_method_summary
				""")
				.map((row, metadata) -> new ReportSlice(0, 0, BigDecimal.ZERO, 0, 0, 0,
						longValue(row, "total_successful_transactions"), null))
				.one();
	}

	@Override
	public Mono<ReportSlice> channelSalesSummary() {
		return databaseClient.sql("""
				SELECT channel
				FROM analyticsflow_olap.channel_sales_summary
				GROUP BY channel
				ORDER BY SUM(total_revenue) DESC NULLS LAST
				LIMIT 1
				""")
				.map((row, metadata) -> new ReportSlice(0, 0, BigDecimal.ZERO, 0, 0, 0, 0,
						row.get("channel", String.class)))
				.one()
				.defaultIfEmpty(ReportSlice.empty());
	}

	private long longValue(Row row, String column) {
		Number value = row.get(column, Number.class);
		return value == null ? 0 : value.longValue();
	}

	private BigDecimal bigDecimalValue(Row row, String column) {
		BigDecimal value = row.get(column, BigDecimal.class);
		return value == null ? BigDecimal.ZERO : value;
	}
}
