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
@Table(schema = DbSchemas.STAGING, name = "stg_product_categories", indexes = {
		@Index(name = "idx_stg_product_categories_job_id", columnList = "job_id"),
		@Index(name = "idx_stg_product_categories_category_id", columnList = "category_id")
})
public class ProductCategoryStagingEntity extends BaseStagingEntity {

	@Column(name = "category_id", length = 64)
	private String categoryId;

	@Column(name = "category_name", length = 255)
	private String categoryName;

	@Column(name = "parent_category_id", length = 64)
	private String parentCategoryId;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	@Column(name = "source_created_at", length = 64)
	private String sourceCreatedAt;
}
