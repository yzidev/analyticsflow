package com.yzidev.analyticsflow.dto.response;

import java.math.BigDecimal;

import com.yzidev.analyticsflow.common.enums.ReportStrategy;

public record ReportBenchmarkResponse(
		ReportStrategy strategy,
		long totalOrders,
		long totalItemsSold,
		BigDecimal totalRevenue,
		long totalCustomers,
		long totalProducts,
		long totalDeliveredOrders,
		long totalSuccessfulTransactions,
		String topChannel,
		long processingTimeMs,
		String threadName,
		String databaseProfile) {
}
