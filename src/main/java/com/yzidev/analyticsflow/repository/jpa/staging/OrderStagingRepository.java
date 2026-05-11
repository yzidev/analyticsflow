package com.yzidev.analyticsflow.repository.jpa.staging;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.staging.OrderStagingEntity;

public interface OrderStagingRepository extends JpaRepository<OrderStagingEntity, Long> {

	List<OrderStagingEntity> findByJobId(String jobId);

	List<OrderStagingEntity> findByJobIdAndValidTrue(String jobId);

	long countByJobId(String jobId);

	void deleteByJobId(String jobId);
}
