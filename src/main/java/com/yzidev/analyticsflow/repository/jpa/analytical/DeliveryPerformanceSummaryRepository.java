package com.yzidev.analyticsflow.repository.jpa.analytical;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.analytical.DeliveryPerformanceSummaryEntity;

public interface DeliveryPerformanceSummaryRepository extends JpaRepository<DeliveryPerformanceSummaryEntity, Long> {

	List<DeliveryPerformanceSummaryEntity> findBySummaryDateBetween(LocalDate startDate, LocalDate endDate);

	List<DeliveryPerformanceSummaryEntity> findByCourierName(String courierName);
}
