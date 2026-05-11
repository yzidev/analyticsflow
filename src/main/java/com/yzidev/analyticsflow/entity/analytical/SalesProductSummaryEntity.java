package com.yzidev.analyticsflow.entity.analytical;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sales_product_summary", indexes = {
		@Index(name = "idx_sales_product_summary_product_id", columnList = "product_id")
})
public class SalesProductSummaryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "product_id", nullable = false, length = 64)
	private String productId;

	@Column(name = "product_name", nullable = false, length = 255)
	private String productName;

	@Column(name = "category_id", length = 64)
	private String categoryId;

	@Column(name = "category_name", length = 255)
	private String categoryName;

	@Column(name = "brand", length = 128)
	private String brand;

	@Column(name = "total_orders", nullable = false)
	private Long totalOrders;

	@Column(name = "total_quantity_sold", nullable = false)
	private Long totalQuantitySold;

	@Column(name = "total_revenue", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalRevenue;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();
}
