package com.yzidev.analyticsflow.entity.transactional;

import java.time.LocalDateTime;

import com.yzidev.analyticsflow.config.DbSchemas;

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
@Table(schema = DbSchemas.OLTP, name = "product_categories", indexes = {
		@Index(name = "idx_product_categories_category_id", columnList = "category_id"),
		@Index(name = "idx_product_categories_parent_category_id", columnList = "parent_category_id")
})
public class ProductCategoryEntity {

	@Id
	@Column(name = "category_id", nullable = false, length = 64)
	private String categoryId;

	@Column(name = "category_name", nullable = false, length = 255)
	private String categoryName;

	@Column(name = "parent_category_id", length = 64)
	private String parentCategoryId;

	@Column(name = "description", columnDefinition = "text")
	private String description;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_category_id", referencedColumnName = "category_id", insertable = false, updatable = false)
	private ProductCategoryEntity parentCategory;
}
