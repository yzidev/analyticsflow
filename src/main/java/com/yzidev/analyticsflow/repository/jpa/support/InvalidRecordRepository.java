package com.yzidev.analyticsflow.repository.jpa.support;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.support.InvalidRecordEntity;

public interface InvalidRecordRepository extends JpaRepository<InvalidRecordEntity, Long> {

	List<InvalidRecordEntity> findByJobId(String jobId);

	List<InvalidRecordEntity> findByJobIdAndSourceFile(String jobId, String sourceFile);

	long countByJobId(String jobId);
}
