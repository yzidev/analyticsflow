package com.yzidev.analyticsflow.repository.jpa.support;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.common.enums.ReportGenerationStatus;
import com.yzidev.analyticsflow.common.enums.ReportType;
import com.yzidev.analyticsflow.entity.support.ReportMetadataEntity;

public interface ReportMetadataRepository extends JpaRepository<ReportMetadataEntity, Long> {

	Optional<ReportMetadataEntity> findByReportId(String reportId);

	List<ReportMetadataEntity> findByJobId(String jobId);

	List<ReportMetadataEntity> findByReportType(ReportType reportType);

	List<ReportMetadataEntity> findByStatus(ReportGenerationStatus status);
}
