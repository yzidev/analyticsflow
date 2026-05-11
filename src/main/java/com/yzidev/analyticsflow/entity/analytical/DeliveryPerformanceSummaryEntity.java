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
@Table(name = "delivery_performance_summary", indexes = {
		@Index(name = "idx_delivery_performance_summary_summary_date", columnList = "summary_date"),
		@Index(name = "idx_delivery_performance_summary_courier_name", columnList = "courier_name")
})
public class DeliveryPerformanceSummaryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "summary_date", nullable = false)
	private LocalDate summaryDate;

	@Column(name = "courier_name", nullable = false, length = 128)
	private String courierName;

	@Column(name = "total_shipments", nullable = false)
	private Long totalShipments;

	@Column(name = "total_pending", nullable = false)
	private Long totalPending;

	@Column(name = "total_shipped", nullable = false)
	private Long totalShipped;

	@Column(name = "total_in_transit", nullable = false)
	private Long totalInTransit;

	@Column(name = "total_delivered", nullable = false)
	private Long totalDelivered;

	@Column(name = "total_failed", nullable = false)
	private Long totalFailed;

	@Column(name = "total_returned", nullable = false)
	private Long totalReturned;

	@Column(name = "average_delivery_duration_hours", precision = 19, scale = 2)
	private BigDecimal averageDeliveryDurationHours;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();
}
