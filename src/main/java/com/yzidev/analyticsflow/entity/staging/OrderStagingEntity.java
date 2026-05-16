package com.yzidev.analyticsflow.entity.staging;

import com.yzidev.analyticsflow.config.DbSchemas;

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
@Table(schema = DbSchemas.STAGING, name = "stg_orders", indexes = {
		@Index(name = "idx_stg_orders_job_id", columnList = "job_id"),
		@Index(name = "idx_stg_orders_order_id", columnList = "order_id"),
		@Index(name = "idx_stg_orders_user_id", columnList = "user_id")
})
public class OrderStagingEntity extends BaseStagingEntity {

	@Column(name = "order_id", length = 64)
	private String orderId;

	@Column(name = "user_id", length = 64)
	private String userId;

	@Column(name = "order_date", length = 64)
	private String orderDate;

	@Column(name = "order_status", length = 32)
	private String orderStatus;

	@Column(name = "total_amount", length = 64)
	private String totalAmount;

	@Column(name = "channel", length = 32)
	private String channel;

	@Column(name = "source_created_at", length = 64)
	private String sourceCreatedAt;
}
