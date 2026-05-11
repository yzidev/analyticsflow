package com.yzidev.analyticsflow.repository.jpa.support;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.common.enums.EtlJobStatus;
import com.yzidev.analyticsflow.entity.support.EtlJobEntity;

public interface EtlJobRepository extends JpaRepository<EtlJobEntity, Long> {

	Optional<EtlJobEntity> findByJobId(String jobId);

	List<EtlJobEntity> findByStatus(EtlJobStatus status);

	boolean existsByJobId(String jobId);
}
