package com.yzidev.analyticsflow.entity.transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
@Table(name = "products", indexes = {
		@Index(name = "idx_products_product_id", columnList = "product_id"),
		@Index(name = "idx_products_category_id", columnList = "category_id")
})
public class ProductEntity {

	@Id
	@Column(name = "product_id", nullable = false, length = 64)
	private String productId;

	@Column(name = "category_id", nullable = false, length = 64)
	private String categoryId;

	@Column(name = "product_name", nullable = false, length = 255)
	private String productName;

	@Column(name = "brand", length = 128)
	private String brand;

	@Column(name = "price", nullable = false, precision = 19, scale = 2)
	private BigDecimal price;

	@Column(name = "is_active", nullable = false)
	private Boolean active;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", referencedColumnName = "category_id", insertable = false, updatable = false)
	private ProductCategoryEntity category;
}
