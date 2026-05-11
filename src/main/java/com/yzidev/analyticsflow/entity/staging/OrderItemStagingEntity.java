package com.yzidev.analyticsflow.entity.staging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stg_order_items", indexes = {
		@Index(name = "idx_stg_order_items_job_id", columnList = "job_id"),
		@Index(name = "idx_stg_order_items_order_item_id", columnList = "order_item_id"),
		@Index(name = "idx_stg_order_items_order_id", columnList = "order_id"),
		@Index(name = "idx_stg_order_items_product_id", columnList = "product_id")
})
public class OrderItemStagingEntity extends BaseStagingEntity {

	@Column(name = "order_item_id", length = 64)
	private String orderItemId;

	@Column(name = "order_id", length = 64)
	private String orderId;

	@Column(name = "product_id", length = 64)
	private String productId;

	@Column(name = "quantity", length = 64)
	private String quantity;

	@Column(name = "unit_price", length = 64)
	private String unitPrice;

	@Column(name = "total_price", length = 64)
	private String totalPrice;
}
