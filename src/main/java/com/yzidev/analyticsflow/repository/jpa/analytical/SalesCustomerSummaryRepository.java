package com.yzidev.analyticsflow.repository.jpa.analytical;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.analytical.SalesCustomerSummaryEntity;

public interface SalesCustomerSummaryRepository extends JpaRepository<SalesCustomerSummaryEntity, Long> {

	Optional<SalesCustomerSummaryEntity> findByUserId(String userId);

	List<SalesCustomerSummaryEntity> findTop50ByOrderByTotalSpentDesc();
}
