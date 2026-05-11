package com.yzidev.analyticsflow.repository.jpa.staging;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.staging.UserStagingEntity;

public interface UserStagingRepository extends JpaRepository<UserStagingEntity, Long> {

	List<UserStagingEntity> findByJobId(String jobId);

	List<UserStagingEntity> findByJobIdAndValidTrue(String jobId);

	long countByJobId(String jobId);

	void deleteByJobId(String jobId);
}
