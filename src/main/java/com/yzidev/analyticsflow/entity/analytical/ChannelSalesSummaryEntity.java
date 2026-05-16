package com.yzidev.analyticsflow.entity.analytical;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.yzidev.analyticsflow.common.enums.SalesChannel;

import com.yzidev.analyticsflow.config.DbSchemas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(schema = DbSchemas.OLAP, name = "channel_sales_summary", indexes = {
		@Index(name = "idx_channel_sales_summary_summary_date", columnList = "summary_date"),
		@Index(name = "idx_channel_sales_summary_channel", columnList = "channel")
})
public class ChannelSalesSummaryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "summary_date", nullable = false)
	private LocalDate summaryDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "channel", nullable = false, length = 32)
	private SalesChannel channel;

	@Column(name = "total_orders", nullable = false)
	private Long totalOrders;

	@Column(name = "total_items_sold", nullable = false)
	private Long totalItemsSold;

	@Column(name = "total_revenue", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalRevenue;

	@Column(name = "total_success_transactions", nullable = false)
	private Long totalSuccessTransactions;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();
}
