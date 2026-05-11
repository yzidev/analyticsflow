package com.yzidev.analyticsflow.repository.jpa.transactional;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.common.enums.TransactionStatus;
import com.yzidev.analyticsflow.entity.transactional.TransactionEntity;

public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {

	List<TransactionEntity> findByOrderId(String orderId);

	List<TransactionEntity> findByUserId(String userId);

	List<TransactionEntity> findByStatus(TransactionStatus status);

	List<TransactionEntity> findByTransactionDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}
