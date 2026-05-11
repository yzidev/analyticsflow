package com.yzidev.analyticsflow.repository.jpa.support;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.common.enums.EtlStepName;
import com.yzidev.analyticsflow.entity.support.EtlJobStepEntity;

public interface EtlJobStepRepository extends JpaRepository<EtlJobStepEntity, Long> {

	List<EtlJobStepEntity> findByJobIdOrderByIdAsc(String jobId);

	Optional<EtlJobStepEntity> findByJobIdAndStepName(String jobId, EtlStepName stepName);
}
