package com.yzidev.analyticsflow.repository.blocking;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.yzidev.analyticsflow.dto.response.ReportSlice;

@Repository
public class JdbcAnalyticsReportRepository implements BlockingAnalyticsReportRepository {

	private final JdbcTemplate jdbcTemplate;

	public JdbcAnalyticsReportRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	public ReportSlice salesDailySummary() {
		return jdbcTemplate.queryForObject("""
				SELECT
					COALESCE(SUM(total_orders), 0) AS total_orders,
					COALESCE(SUM(total_items_sold), 0) AS total_items_sold,
					COALESCE(SUM(total_gross_revenue), 0) AS total_revenue,
					COALESCE(SUM(total_delivered_orders), 0) AS total_delivered_orders,
					COALESCE(SUM(total_success_transactions), 0) AS total_successful_transactions
				FROM analyticsflow_olap.sales_daily_summary
				""", (rs, rowNum) -> new ReportSlice(
				rs.getLong("total_orders"),
				rs.getLong("total_items_sold"),
				rs.getBigDecimal("total_revenue"),
				0,
				0,
				rs.getLong("total_delivered_orders"),
				rs.getLong("total_successful_transactions"),
				null));
	}

	@Override
	public ReportSlice salesProductSummary() {
		return jdbcTemplate.queryForObject("""
				SELECT COALESCE(COUNT(DISTINCT product_id), 0) AS total_products
				FROM analyticsflow_olap.sales_product_summary
				""", (rs, rowNum) -> new ReportSlice(0, 0, BigDecimal.ZERO, 0,
				rs.getLong("total_products"), 0, 0, null));
	}

	@Override
	public ReportSlice salesCustomerSummary() {
		return jdbcTemplate.queryForObject("""
				SELECT COALESCE(COUNT(DISTINCT user_id), 0) AS total_customers
				FROM analyticsflow_olap.sales_customer_summary
				""", (rs, rowNum) -> new ReportSlice(0, 0, BigDecimal.ZERO,
				rs.getLong("total_customers"), 0, 0, 0, null));
	}

	@Override
	public ReportSlice deliveryPerformanceSummary() {
		return jdbcTemplate.queryForObject("""
				SELECT COALESCE(SUM(total_delivered), 0) AS total_delivered_orders
				FROM analyticsflow_olap.delivery_performance_summary
				""", (rs, rowNum) -> new ReportSlice(0, 0, BigDecimal.ZERO, 0, 0,
				rs.getLong("total_delivered_orders"), 0, null));
	}

	@Override
	public ReportSlice paymentMethodSummary() {
		return jdbcTemplate.queryForObject("""
				SELECT COALESCE(SUM(total_success), 0) AS total_successful_transactions
				FROM analyticsflow_olap.payment_method_summary
				""", (rs, rowNum) -> new ReportSlice(0, 0, BigDecimal.ZERO, 0, 0, 0,
				rs.getLong("total_successful_transactions"), null));
	}

	@Override
	public ReportSlice channelSalesSummary() {
		List<ReportSlice> slices = jdbcTemplate.query("""
				SELECT channel
				FROM analyticsflow_olap.channel_sales_summary
				GROUP BY channel
				ORDER BY SUM(total_revenue) DESC NULLS LAST
				LIMIT 1
				""", this::channelSlice);
		return slices.isEmpty() ? ReportSlice.empty() : slices.getFirst();
	}

	private ReportSlice channelSlice(ResultSet rs, int rowNum) throws SQLException {
		return new ReportSlice(0, 0, BigDecimal.ZERO, 0, 0, 0, 0, rs.getString("channel"));
	}
}
