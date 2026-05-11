package com.yzidev.analyticsflow.repository.jpa.analytical;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.common.enums.PaymentMethod;
import com.yzidev.analyticsflow.entity.analytical.PaymentMethodSummaryEntity;

public interface PaymentMethodSummaryRepository extends JpaRepository<PaymentMethodSummaryEntity, Long> {

	List<PaymentMethodSummaryEntity> findBySummaryDateBetween(LocalDate startDate, LocalDate endDate);

	List<PaymentMethodSummaryEntity> findByPaymentMethod(PaymentMethod paymentMethod);
}
