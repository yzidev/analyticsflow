package com.yzidev.analyticsflow.entity.transactional;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "order_items", indexes = {
		@Index(name = "idx_order_items_order_item_id", columnList = "order_item_id"),
		@Index(name = "idx_order_items_order_id", columnList = "order_id"),
		@Index(name = "idx_order_items_product_id", columnList = "product_id")
})
public class OrderItemEntity {

	@Id
	@Column(name = "order_item_id", nullable = false, length = 64)
	private String orderItemId;

	@Column(name = "order_id", nullable = false, length = 64)
	private String orderId;

	@Column(name = "product_id", nullable = false, length = 64)
	private String productId;

	@Column(name = "quantity", nullable = false)
	private Integer quantity;

	@Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal unitPrice;

	@Column(name = "total_price", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalPrice;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", referencedColumnName = "order_id", insertable = false, updatable = false)
	private OrderEntity order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", referencedColumnName = "product_id", insertable = false, updatable = false)
	private ProductEntity product;
}
