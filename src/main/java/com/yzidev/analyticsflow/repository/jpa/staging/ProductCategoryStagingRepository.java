package com.yzidev.analyticsflow.repository.jpa.staging;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.staging.ProductCategoryStagingEntity;

public interface ProductCategoryStagingRepository extends JpaRepository<ProductCategoryStagingEntity, Long> {

	List<ProductCategoryStagingEntity> findByJobId(String jobId);

	List<ProductCategoryStagingEntity> findByJobIdAndValidTrue(String jobId);

	long countByJobId(String jobId);

	void deleteByJobId(String jobId);
}
