package com.yzidev.analyticsflow.entity.transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.yzidev.analyticsflow.common.enums.PaymentMethod;
import com.yzidev.analyticsflow.common.enums.TransactionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "transactions", indexes = {
		@Index(name = "idx_transactions_transaction_id", columnList = "transaction_id"),
		@Index(name = "idx_transactions_order_id", columnList = "order_id"),
		@Index(name = "idx_transactions_user_id", columnList = "user_id"),
		@Index(name = "idx_transactions_transaction_date", columnList = "transaction_date"),
		@Index(name = "idx_transactions_status", columnList = "status")
})
public class TransactionEntity {

	@Id
	@Column(name = "transaction_id", nullable = false, length = 64)
	private String transactionId;

	@Column(name = "order_id", nullable = false, length = 64)
	private String orderId;

	@Column(name = "user_id", nullable = false, length = 64)
	private String userId;

	@Column(name = "transaction_date", nullable = false)
	private LocalDateTime transactionDate;

	@Enumerated(EnumType.STRING)
	@Column(name = "payment_method", nullable = false, length = 32)
	private PaymentMethod paymentMethod;

	@Column(name = "amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal amount;

	@Column(name = "currency", nullable = false, length = 8)
	private String currency;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private TransactionStatus status;

	@Column(name = "created_at")
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", referencedColumnName = "order_id", insertable = false, updatable = false)
	private OrderEntity order;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", referencedColumnName = "user_id", insertable = false, updatable = false)
	private UserEntity user;
}
