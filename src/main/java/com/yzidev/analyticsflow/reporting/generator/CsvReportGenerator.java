package com.yzidev.analyticsflow.reporting.generator;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.yzidev.analyticsflow.common.enums.ReportType;

@Component
public class CsvReportGenerator {

	private final JdbcTemplate jdbcTemplate;

	public CsvReportGenerator(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public String generate(ReportType reportType, LocalDate startDate, LocalDate endDate) {
		return switch (reportType) {
			case SALES_DAILY_SUMMARY -> generateSalesDailySummary(startDate, endDate);
			case SALES_PRODUCT_SUMMARY -> generateSalesProductSummary();
			case SALES_CUSTOMER_SUMMARY -> generateSalesCustomerSummary();
			case DELIVERY_PERFORMANCE_SUMMARY -> generateDeliveryPerformanceSummary(startDate, endDate);
			case PAYMENT_METHOD_SUMMARY -> generatePaymentMethodSummary(startDate, endDate);
			case CHANNEL_SALES_SUMMARY -> generateChannelSalesSummary(startDate, endDate);
		};
	}

	private String generateSalesDailySummary(LocalDate startDate, LocalDate endDate) {
		return csv(
				List.of("summary_date", "total_orders", "total_items_sold", "total_gross_revenue",
						"total_paid_revenue", "total_success_transactions", "total_failed_transactions",
						"total_shipped_orders", "total_delivered_orders"),
				jdbcTemplate.queryForList("""
						select summary_date, total_orders, total_items_sold, total_gross_revenue,
						       total_paid_revenue, total_success_transactions, total_failed_transactions,
						       total_shipped_orders, total_delivered_orders
						from sales_daily_summary
						where (?::date is null or summary_date >= ?::date)
						  and (?::date is null or summary_date <= ?::date)
						order by summary_date
						""", startDate, startDate, endDate, endDate));
	}

	private String generateSalesProductSummary() {
		return csv(
				List.of("product_id", "product_name", "category_id", "category_name", "brand",
						"total_orders", "total_quantity_sold", "total_revenue"),
				jdbcTemplate.queryForList("""
						select product_id, product_name, category_id, category_name, brand,
						       total_orders, total_quantity_sold, total_revenue
						from sales_product_summary
						order by total_revenue desc, product_id
						"""));
	}

	private String generateSalesCustomerSummary() {
		return csv(
				List.of("user_id", "full_name", "email", "city", "country", "total_orders",
						"total_items_purchased", "total_spent", "last_order_date"),
				jdbcTemplate.queryForList("""
						select user_id, full_name, email, city, country, total_orders,
						       total_items_purchased, total_spent, last_order_date
						from sales_customer_summary
						order by total_spent desc, user_id
						"""));
	}

	private String generateDeliveryPerformanceSummary(LocalDate startDate, LocalDate endDate) {
		return csv(
				List.of("summary_date", "courier_name", "total_shipments", "total_pending", "total_shipped",
						"total_in_transit", "total_delivered", "total_failed", "total_returned",
						"average_delivery_duration_hours"),
				jdbcTemplate.queryForList("""
						select summary_date, courier_name, total_shipments, total_pending, total_shipped,
						       total_in_transit, total_delivered, total_failed, total_returned,
						       average_delivery_duration_hours
						from delivery_performance_summary
						where (?::date is null or summary_date >= ?::date)
						  and (?::date is null or summary_date <= ?::date)
						order by summary_date, courier_name
						""", startDate, startDate, endDate, endDate));
	}

	private String generatePaymentMethodSummary(LocalDate startDate, LocalDate endDate) {
		return csv(
				List.of("summary_date", "payment_method", "currency", "total_transactions",
						"total_success", "total_failed", "total_pending", "total_amount"),
				jdbcTemplate.queryForList("""
						select summary_date, payment_method, currency, total_transactions,
						       total_success, total_failed, total_pending, total_amount
						from payment_method_summary
						where (?::date is null or summary_date >= ?::date)
						  and (?::date is null or summary_date <= ?::date)
						order by summary_date, payment_method, currency
						""", startDate, startDate, endDate, endDate));
	}

	private String generateChannelSalesSummary(LocalDate startDate, LocalDate endDate) {
		return csv(
				List.of("summary_date", "channel", "total_orders", "total_items_sold",
						"total_revenue", "total_success_transactions"),
				jdbcTemplate.queryForList("""
						select summary_date, channel, total_orders, total_items_sold,
						       total_revenue, total_success_transactions
						from channel_sales_summary
						where (?::date is null or summary_date >= ?::date)
						  and (?::date is null or summary_date <= ?::date)
						order by summary_date, channel
						""", startDate, startDate, endDate, endDate));
	}

	private String csv(List<String> headers, List<Map<String, Object>> rows) {
		StringBuilder builder = new StringBuilder();
		builder.append(String.join(",", headers)).append(System.lineSeparator());
		for (Map<String, Object> row : rows) {
			builder.append(headers.stream()
					.map(header -> escape(row.get(header)))
					.collect(Collectors.joining(",")))
					.append(System.lineSeparator());
		}
		return builder.toString();
	}

	private String escape(Object value) {
		if (value == null) {
			return "";
		}
		String text = value.toString();
		if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
			return "\"" + text.replace("\"", "\"\"") + "\"";
		}
		return text;
	}
}
