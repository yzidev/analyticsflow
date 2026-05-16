package com.yzidev.analyticsflow.entity.transactional;

import java.time.LocalDateTime;

import com.yzidev.analyticsflow.common.enums.DeliveryStatus;

import com.yzidev.analyticsflow.config.DbSchemas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = DbSchemas.OLTP, name = "deliveries", indexes = {
		@Index(name = "idx_deliveries_delivery_id", columnList = "delivery_id"),
		@Index(name = "idx_deliveries_order_id", columnList = "order_id"),
		@Index(name = "idx_deliveries_delivery_status", columnList = "delivery_status")
})
public class DeliveryEntity {

	@Id
	@Column(name = "delivery_id", nullable = false, length = 64)
	private String deliveryId;

	@Column(name = "order_id", nullable = false, length = 64)
	private String orderId;

	@Enumerated(EnumType.STRING)
	@Column(name = "delivery_status", nullable = false, length = 32)
	private DeliveryStatus deliveryStatus;

	@Column(name = "delivery_address", nullable = false, columnDefinition = "text")
	private String deliveryAddress;

	@Column(name = "shipped_date")
	private LocalDateTime shippedDate;

	@Column(name = "delivered_date")
	private LocalDateTime deliveredDate;

	@Column(name = "courier_name", nullable = false, length = 128)
	private String courierName;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", referencedColumnName = "order_id", insertable = false, updatable = false)
	private OrderEntity order;
}
