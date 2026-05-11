package com.yzidev.analyticsflow.dto.response;

import java.math.BigDecimal;

public record ReportSlice(
		long totalOrders,
		long totalItemsSold,
		BigDecimal totalRevenue,
		long totalCustomers,
		long totalProducts,
		long totalDeliveredOrders,
		long totalSuccessfulTransactions,
		String topChannel) {

	public static ReportSlice empty() {
		return new ReportSlice(0, 0, BigDecimal.ZERO, 0, 0, 0, 0, null);
	}

	public ReportSlice merge(ReportSlice other) {
		String channel = other.topChannel != null ? other.topChannel : topChannel;
		return new ReportSlice(
				totalOrders + other.totalOrders,
				totalItemsSold + other.totalItemsSold,
				totalRevenue.add(other.totalRevenue),
				totalCustomers + other.totalCustomers,
				totalProducts + other.totalProducts,
				totalDeliveredOrders + other.totalDeliveredOrders,
				totalSuccessfulTransactions + other.totalSuccessfulTransactions,
				channel);
	}
}
