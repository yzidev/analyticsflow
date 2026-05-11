package com.yzidev.analyticsflow.repository.jpa.analytical;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.analytical.SalesProductSummaryEntity;

public interface SalesProductSummaryRepository extends JpaRepository<SalesProductSummaryEntity, Long> {

	Optional<SalesProductSummaryEntity> findByProductId(String productId);

	List<SalesProductSummaryEntity> findTop50ByOrderByTotalRevenueDesc();
}
