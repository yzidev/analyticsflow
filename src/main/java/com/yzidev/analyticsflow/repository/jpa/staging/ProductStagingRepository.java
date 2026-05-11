package com.yzidev.analyticsflow.repository.jpa.staging;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.staging.ProductStagingEntity;

public interface ProductStagingRepository extends JpaRepository<ProductStagingEntity, Long> {

	List<ProductStagingEntity> findByJobId(String jobId);

	List<ProductStagingEntity> findByJobIdAndValidTrue(String jobId);

	long countByJobId(String jobId);

	void deleteByJobId(String jobId);
}
