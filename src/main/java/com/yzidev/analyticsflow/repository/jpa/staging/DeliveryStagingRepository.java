package com.yzidev.analyticsflow.repository.jpa.staging;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.staging.DeliveryStagingEntity;

public interface DeliveryStagingRepository extends JpaRepository<DeliveryStagingEntity, Long> {

	List<DeliveryStagingEntity> findByJobId(String jobId);

	List<DeliveryStagingEntity> findByJobIdAndValidTrue(String jobId);

	long countByJobId(String jobId);

	void deleteByJobId(String jobId);
}
