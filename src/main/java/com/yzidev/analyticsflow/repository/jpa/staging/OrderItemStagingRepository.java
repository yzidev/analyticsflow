package com.yzidev.analyticsflow.repository.jpa.staging;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.staging.OrderItemStagingEntity;

public interface OrderItemStagingRepository extends JpaRepository<OrderItemStagingEntity, Long> {

	List<OrderItemStagingEntity> findByJobId(String jobId);

	List<OrderItemStagingEntity> findByJobIdAndValidTrue(String jobId);

	long countByJobId(String jobId);

	void deleteByJobId(String jobId);
}
