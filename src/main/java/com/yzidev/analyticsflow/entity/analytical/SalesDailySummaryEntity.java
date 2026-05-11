package com.yzidev.analyticsflow.entity.analytical;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sales_daily_summary", indexes = {
		@Index(name = "idx_sales_daily_summary_summary_date", columnList = "summary_date")
})
public class SalesDailySummaryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "summary_date", nullable = false)
	private LocalDate summaryDate;

	@Column(name = "total_orders", nullable = false)
	private Long totalOrders;

	@Column(name = "total_items_sold", nullable = false)
	private Long totalItemsSold;

	@Column(name = "total_gross_revenue", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalGrossRevenue;

	@Column(name = "total_paid_revenue", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalPaidRevenue;

	@Column(name = "total_success_transactions", nullable = false)
	private Long totalSuccessTransactions;

	@Column(name = "total_failed_transactions", nullable = false)
	private Long totalFailedTransactions;

	@Column(name = "total_shipped_orders", nullable = false)
	private Long totalShippedOrders;

	@Column(name = "total_delivered_orders", nullable = false)
	private Long totalDeliveredOrders;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();
}
