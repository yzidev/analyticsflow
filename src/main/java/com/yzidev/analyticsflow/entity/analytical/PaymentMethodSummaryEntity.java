package com.yzidev.analyticsflow.entity.analytical;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.yzidev.analyticsflow.common.enums.PaymentMethod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "payment_method_summary", indexes = {
		@Index(name = "idx_payment_method_summary_summary_date", columnList = "summary_date"),
		@Index(name = "idx_payment_method_summary_payment_method", columnList = "payment_method")
})
public class PaymentMethodSummaryEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "summary_date", nullable = false)
	private LocalDate summaryDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 32)
	private PaymentMethod paymentMethod;

	@Column(name = "currency", nullable = false, length = 8)
	private String currency;

	@Column(name = "total_transactions", nullable = false)
	private Long totalTransactions;

	@Column(name = "total_success", nullable = false)
	private Long totalSuccess;

	@Column(name = "total_failed", nullable = false)
	private Long totalFailed;

	@Column(name = "total_pending", nullable = false)
	private Long totalPending;

	@Column(name = "total_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();
}
