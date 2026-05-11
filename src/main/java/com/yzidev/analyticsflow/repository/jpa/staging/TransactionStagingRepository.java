package com.yzidev.analyticsflow.repository.jpa.staging;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.staging.TransactionStagingEntity;

public interface TransactionStagingRepository extends JpaRepository<TransactionStagingEntity, Long> {

	List<TransactionStagingEntity> findByJobId(String jobId);

	List<TransactionStagingEntity> findByJobIdAndValidTrue(String jobId);

	long countByJobId(String jobId);

	void deleteByJobId(String jobId);
}
