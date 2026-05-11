package com.yzidev.analyticsflow.repository.jpa.staging;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.staging.ProductDetailStagingEntity;

public interface ProductDetailStagingRepository extends JpaRepository<ProductDetailStagingEntity, Long> {

	List<ProductDetailStagingEntity> findByJobId(String jobId);

	List<ProductDetailStagingEntity> findByJobIdAndValidTrue(String jobId);

	long countByJobId(String jobId);

	void deleteByJobId(String jobId);
}
