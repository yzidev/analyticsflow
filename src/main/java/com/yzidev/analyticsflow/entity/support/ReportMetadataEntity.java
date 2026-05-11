package com.yzidev.analyticsflow.entity.support;

import java.time.LocalDateTime;

import com.yzidev.analyticsflow.common.enums.ReportFormat;
import com.yzidev.analyticsflow.common.enums.ReportGenerationStatus;
import com.yzidev.analyticsflow.common.enums.ReportType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "report_metadata", indexes = {
		@Index(name = "idx_report_metadata_report_id", columnList = "report_id", unique = true),
		@Index(name = "idx_report_metadata_job_id", columnList = "job_id"),
		@Index(name = "idx_report_metadata_report_type", columnList = "report_type")
})
public class ReportMetadataEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "report_id", nullable = false, unique = true, length = 64)
	private String reportId;

	@Column(name = "job_id", length = 64)
	private String jobId;

	@Enumerated(EnumType.STRING)
	@Column(name = "report_type", nullable = false, length = 64)
	private ReportType reportType;

	@Column(name = "file_name", nullable = false, length = 255)
	private String fileName;

	@Column(name = "file_path", nullable = false, length = 1024)
	private String filePath;

	@Enumerated(EnumType.STRING)
	@Column(name = "format", nullable = false, length = 16)
	private ReportFormat format;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private ReportGenerationStatus status;

	@Column(name = "generated_at")
	private LocalDateTime generatedAt;

	@Column(name = "duration_ms")
	private Long durationMs;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();
}
