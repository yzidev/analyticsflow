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
@Table(schema = DbSchemas.STAGING, name = "stg_products", indexes = {
		@Index(name = "idx_stg_products_job_id", columnList = "job_id"),
		@Index(name = "idx_stg_products_product_id", columnList = "product_id"),
		@Index(name = "idx_stg_products_category_id", columnList = "category_id")
})
public class ProductStagingEntity extends BaseStagingEntity {

	@Column(name = "product_id", length = 64)
	private String productId;

	@Column(name = "category_id", length = 64)
	private String categoryId;

	@Column(name = "product_name", length = 255)
	private String productName;

	@Column(name = "brand", length = 128)
	private String brand;

	@Column(name = "price", length = 64)
	private String price;

	@Column(name = "is_active", length = 16)
	private String active;

	@Column(name = "source_created_at", length = 64)
	private String sourceCreatedAt;
}
