package com.yzidev.analyticsflow.repository.jpa.analytical;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.analytical.SalesDailySummaryEntity;

public interface SalesDailySummaryRepository extends JpaRepository<SalesDailySummaryEntity, Long> {

	Optional<SalesDailySummaryEntity> findBySummaryDate(LocalDate summaryDate);

	List<SalesDailySummaryEntity> findBySummaryDateBetween(LocalDate startDate, LocalDate endDate);
}
