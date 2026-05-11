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
@Table(name = "sales_customer_summary", indexes = {
		@Index(name = "idx_sales_customer_summary_user_id", columnList = "user_id")
})
public class SalesCustomerSummaryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, length = 64)
	private String userId;

	@Column(name = "full_name", nullable = false, length = 255)
	private String fullName;

	@Column(name = "email", length = 255)
	private String email;

	@Column(name = "city", length = 128)
	private String city;

	@Column(name = "country", length = 128)
	private String country;

	@Column(name = "total_orders", nullable = false)
	private Long totalOrders;

	@Column(name = "total_items_purchased", nullable = false)
	private Long totalItemsPurchased;

	@Column(name = "total_spent", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalSpent;

	@Column(name = "last_order_date")
	private LocalDateTime lastOrderDate;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();
}
