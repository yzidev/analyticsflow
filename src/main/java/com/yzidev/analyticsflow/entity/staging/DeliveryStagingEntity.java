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
@Table(schema = DbSchemas.STAGING, name = "stg_deliveries", indexes = {
		@Index(name = "idx_stg_deliveries_job_id", columnList = "job_id"),
		@Index(name = "idx_stg_deliveries_delivery_id", columnList = "delivery_id"),
		@Index(name = "idx_stg_deliveries_order_id", columnList = "order_id")
})
public class DeliveryStagingEntity extends BaseStagingEntity {

	@Column(name = "delivery_id", length = 64)
	private String deliveryId;

	@Column(name = "order_id", length = 64)
	private String orderId;

	@Column(name = "delivery_status", length = 32)
	private String deliveryStatus;

	@Column(name = "delivery_address", columnDefinition = "text")
	private String deliveryAddress;

	@Column(name = "shipped_date", length = 64)
	private String shippedDate;

	@Column(name = "delivered_date", length = 64)
	private String deliveredDate;

	@Column(name = "courier_name", length = 128)
	private String courierName;

	@Column(name = "source_created_at", length = 64)
	private String sourceCreatedAt;
}
