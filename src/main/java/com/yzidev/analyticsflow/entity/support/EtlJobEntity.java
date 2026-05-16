package com.yzidev.analyticsflow.entity.support;

import java.time.LocalDateTime;

import com.yzidev.analyticsflow.common.enums.EtlJobStatus;

import com.yzidev.analyticsflow.config.DbSchemas;

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
@Table(schema = DbSchemas.SUPPORT, name = "etl_job", indexes = {
		@Index(name = "idx_etl_job_job_id", columnList = "job_id", unique = true),
		@Index(name = "idx_etl_job_status", columnList = "status")
})
public class EtlJobEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "job_id", nullable = false, unique = true, length = 64)
	private String jobId;

	@Column(name = "sample_directory", nullable = false, length = 512)
	private String sampleDirectory;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private EtlJobStatus status;

	@Column(name = "total_rows", nullable = false)
	private Long totalRows = 0L;

	@Column(name = "processed_rows", nullable = false)
	private Long processedRows = 0L;

	@Column(name = "success_rows", nullable = false)
	private Long successRows = 0L;

	@Column(name = "failed_rows", nullable = false)
	private Long failedRows = 0L;

	@Column(name = "started_at")
	private LocalDateTime startedAt;

	@Column(name = "finished_at")
	private LocalDateTime finishedAt;

	@Column(name = "duration_ms")
	private Long durationMs;

	@Column(name = "error_message", columnDefinition = "text")
	private String errorMessage;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();

	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt = LocalDateTime.now();
}
