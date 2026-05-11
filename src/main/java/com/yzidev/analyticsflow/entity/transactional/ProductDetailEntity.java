package com.yzidev.analyticsflow.entity.transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table(name = "product_details", indexes = {
		@Index(name = "idx_product_details_product_detail_id", columnList = "product_detail_id"),
		@Index(name = "idx_product_details_product_id", columnList = "product_id"),
		@Index(name = "idx_product_details_sku", columnList = "sku")
})
public class ProductDetailEntity {

	@Id
	@Column(name = "product_detail_id", nullable = false, length = 64)
	private String productDetailId;

	@Column(name = "product_id", nullable = false, length = 64)
	private String productId;

	@Column(name = "sku", nullable = false, length = 128)
	private String sku;

	@Column(name = "color", length = 64)
	private String color;

	@Column(name = "size", length = 64)
	private String size;

	@Column(name = "weight", precision = 19, scale = 4)
	private BigDecimal weight;

	@Column(name = "material", length = 128)
	private String material;

	@Column(name = "manufacture_date")
	private LocalDate manufactureDate;

	@Column(name = "expiry_date")
	private LocalDate expiryDate;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", referencedColumnName = "product_id", insertable = false, updatable = false)
	private ProductEntity product;
}
