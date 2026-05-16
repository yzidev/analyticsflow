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
@Table(schema = DbSchemas.STAGING, name = "stg_product_details", indexes = {
		@Index(name = "idx_stg_product_details_job_id", columnList = "job_id"),
		@Index(name = "idx_stg_product_details_product_detail_id", columnList = "product_detail_id"),
		@Index(name = "idx_stg_product_details_product_id", columnList = "product_id")
})
public class ProductDetailStagingEntity extends BaseStagingEntity {

	@Column(name = "product_detail_id", length = 64)
	private String productDetailId;

	@Column(name = "product_id", length = 64)
	private String productId;

	@Column(name = "sku", length = 128)
	private String sku;

	@Column(name = "color", length = 64)
	private String color;

	@Column(name = "size", length = 64)
	private String size;

	@Column(name = "weight", length = 64)
	private String weight;

	@Column(name = "material", length = 128)
	private String material;

	@Column(name = "manufacture_date", length = 64)
	private String manufactureDate;

	@Column(name = "expiry_date", length = 64)
	private String expiryDate;

	@Column(name = "source_created_at", length = 64)
	private String sourceCreatedAt;
}
