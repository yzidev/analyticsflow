package com.yzidev.analyticsflow.reporting.generator;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Component;

import com.yzidev.analyticsflow.common.enums.ReportType;

@Component
public class CsvReportGenerator {

	private final JdbcTemplate jdbcTemplate;

	public CsvReportGenerator(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void generate(Path reportPath, ReportType reportType, LocalDate startDate, LocalDate endDate)
			throws IOException {
		try (BufferedWriter writer = Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8)) {
			switch (reportType) {
				case SALES_DAILY_SUMMARY -> generateSalesDailySummary(writer, startDate, endDate);
				case SALES_PRODUCT_SUMMARY -> generateSalesProductSummary(writer);
				case SALES_CUSTOMER_SUMMARY -> generateSalesCustomerSummary(writer);
				case DELIVERY_PERFORMANCE_SUMMARY -> generateDeliveryPerformanceSummary(writer, startDate, endDate);
				case PAYMENT_METHOD_SUMMARY -> generatePaymentMethodSummary(writer, startDate, endDate);
				case CHANNEL_SALES_SUMMARY -> generateChannelSalesSummary(writer, startDate, endDate);
			}
		}
		catch (UncheckedIOException exception) {
			throw exception.getCause();
		}
	}

	private void generateSalesDailySummary(BufferedWriter writer, LocalDate startDate, LocalDate endDate) {
		csv(
				writer,
				List.of("summary_date", "total_orders", "total_items_sold", "total_gross_revenue",
						"total_paid_revenue", "total_success_transactions", "total_failed_transactions",
						"total_shipped_orders", "total_delivered_orders"),
				"""
						select summary_date, total_orders, total_items_sold, total_gross_revenue,
						       total_paid_revenue, total_success_transactions, total_failed_transactions,
						       total_shipped_orders, total_delivered_orders
						from analyticsflow_olap.sales_daily_summary
						where (?::date is null or summary_date >= ?::date)
						  and (?::date is null or summary_date <= ?::date)
						order by summary_date
						""",
				startDate, startDate, endDate, endDate);
	}

	private void generateSalesProductSummary(BufferedWriter writer) {
		csv(
				writer,
				List.of("product_id", "product_name", "category_id", "category_name", "brand",
						"total_orders", "total_quantity_sold", "total_revenue"),
				"""
						select product_id, product_name, category_id, category_name, brand,
						       total_orders, total_quantity_sold, total_revenue
						from analyticsflow_olap.sales_product_summary
						order by total_revenue desc, product_id
						""");
	}

	private void generateSalesCustomerSummary(BufferedWriter writer) {
		csv(
				writer,
				List.of("user_id", "full_name", "email", "city", "country", "total_orders",
						"total_items_purchased", "total_spent", "last_order_date"),
				"""
						select user_id, full_name, email, city, country, total_orders,
						       total_items_purchased, total_spent, last_order_date
						from analyticsflow_olap.sales_customer_summary
						order by total_spent desc, user_id
						""");
	}

	private void generateDeliveryPerformanceSummary(BufferedWriter writer, LocalDate startDate, LocalDate endDate) {
		csv(
				writer,
				List.of("summary_date", "courier_name", "total_shipments", "total_pending", "total_shipped",
						"total_in_transit", "total_delivered", "total_failed", "total_returned",
						"average_delivery_duration_hours"),
				"""
						select summary_date, courier_name, total_shipments, total_pending, total_shipped,
						       total_in_transit, total_delivered, total_failed, total_returned,
						       average_delivery_duration_hours
						from analyticsflow_olap.delivery_performance_summary
						where (?::date is null or summary_date >= ?::date)
						  and (?::date is null or summary_date <= ?::date)
						order by summary_date, courier_name
						""",
				startDate, startDate, endDate, endDate);
	}

	private void generatePaymentMethodSummary(BufferedWriter writer, LocalDate startDate, LocalDate endDate) {
		csv(
				writer,
				List.of("summary_date", "payment_method", "currency", "total_transactions",
						"total_success", "total_failed", "total_pending", "total_amount"),
				"""
						select summary_date, payment_method, currency, total_transactions,
						       total_success, total_failed, total_pending, total_amount
						from analyticsflow_olap.payment_method_summary
						where (?::date is null or summary_date >= ?::date)
						  and (?::date is null or summary_date <= ?::date)
						order by summary_date, payment_method, currency
						""",
				startDate, startDate, endDate, endDate);
	}

	private void generateChannelSalesSummary(BufferedWriter writer, LocalDate startDate, LocalDate endDate) {
		csv(
				writer,
				List.of("summary_date", "channel", "total_orders", "total_items_sold",
						"total_revenue", "total_success_transactions"),
				"""
						select summary_date, channel, total_orders, total_items_sold,
						       total_revenue, total_success_transactions
						from analyticsflow_olap.channel_sales_summary
						where (?::date is null or summary_date >= ?::date)
						  and (?::date is null or summary_date <= ?::date)
						order by summary_date, channel
						""",
				startDate, startDate, endDate, endDate);
	}

	private void csv(BufferedWriter writer, List<String> headers, String sql, Object... params) {
		writeLine(writer, String.join(",", headers));
		jdbcTemplate.query(streamingStatement(sql, params), resultSet -> {
			while (resultSet.next()) {
				writeLine(writer, headers.stream()
						.map(header -> resultSetValue(resultSet, header))
						.map(this::escape)
						.collect(Collectors.joining(",")));
			}
			return null;
		});
	}

	private PreparedStatementCreator streamingStatement(String sql, Object... params) {
		return connection -> {
			PreparedStatement statement = connection.prepareStatement(sql);
			statement.setFetchSize(1_000);
			for (int index = 0; index < params.length; index++) {
				statement.setObject(index + 1, params[index]);
			}
			return statement;
		};
	}

	private Object resultSetValue(java.sql.ResultSet resultSet, String columnName) {
		try {
			return resultSet.getObject(columnName);
		}
		catch (SQLException exception) {
			throw new IllegalStateException("Cannot read report column: " + columnName, exception);
		}
	}

	private void writeLine(BufferedWriter writer, String value) {
		try {
			writer.write(value);
			writer.newLine();
		}
		catch (IOException exception) {
			throw new UncheckedIOException(exception);
		}
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
