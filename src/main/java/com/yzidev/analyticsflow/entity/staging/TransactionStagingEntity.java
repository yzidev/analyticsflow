package com.yzidev.analyticsflow.entity.staging;

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
@Table(name = "stg_transactions", indexes = {
		@Index(name = "idx_stg_transactions_job_id", columnList = "job_id"),
		@Index(name = "idx_stg_transactions_transaction_id", columnList = "transaction_id"),
		@Index(name = "idx_stg_transactions_order_id", columnList = "order_id"),
		@Index(name = "idx_stg_transactions_user_id", columnList = "user_id")
})
public class TransactionStagingEntity extends BaseStagingEntity {

	@Column(name = "transaction_id", length = 64)
	private String transactionId;

	@Column(name = "order_id", length = 64)
	private String orderId;

	@Column(name = "user_id", length = 64)
	private String userId;

	@Column(name = "transaction_date", length = 64)
	private String transactionDate;

	@Column(name = "payment_method", length = 32)
	private String paymentMethod;

	@Column(name = "amount", length = 64)
	private String amount;

	@Column(name = "currency", length = 8)
	private String currency;

	@Column(name = "status", length = 32)
	private String status;

	@Column(name = "source_created_at", length = 64)
	private String sourceCreatedAt;
}
