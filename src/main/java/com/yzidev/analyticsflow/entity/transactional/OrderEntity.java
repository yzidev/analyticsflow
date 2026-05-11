package com.yzidev.analyticsflow.entity.transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.yzidev.analyticsflow.common.enums.OrderStatus;
import com.yzidev.analyticsflow.common.enums.SalesChannel;

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
@Table(name = "orders", indexes = {
		@Index(name = "idx_orders_order_id", columnList = "order_id"),
		@Index(name = "idx_orders_user_id", columnList = "user_id"),
		@Index(name = "idx_orders_order_date", columnList = "order_date"),
		@Index(name = "idx_orders_channel", columnList = "channel")
})
public class OrderEntity {

	@Id
	@Column(name = "order_id", nullable = false, length = 64)
	private String orderId;

	@Column(name = "user_id", nullable = false, length = 64)
	private String userId;

	@Column(name = "order_date", nullable = false)
	private LocalDateTime orderDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "order_status", nullable = false, length = 32)
	private OrderStatus orderStatus;

	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "channel", nullable = false, length = 32)
	private SalesChannel channel;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
	private UserEntity user;
}
