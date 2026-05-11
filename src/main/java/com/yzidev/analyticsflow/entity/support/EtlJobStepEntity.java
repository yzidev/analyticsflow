package com.yzidev.analyticsflow.entity.support;

import java.time.LocalDateTime;

import com.yzidev.analyticsflow.common.enums.EtlJobStatus;
import com.yzidev.analyticsflow.common.enums.EtlStepName;

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
@Table(name = "etl_job_step", indexes = {
		@Index(name = "idx_etl_job_step_job_id", columnList = "job_id"),
		@Index(name = "idx_etl_job_step_step_name", columnList = "step_name")
})
public class EtlJobStepEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "job_id", nullable = false, length = 64)
	private String jobId;

	@Enumerated(EnumType.STRING)
	@Column(name = "step_name", nullable = false, length = 64)
	private EtlStepName stepName;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 32)
	private EtlJobStatus status;

	@Column(name = "read_count", nullable = false)
	private Long readCount = 0L;

	@Column(name = "write_count", nullable = false)
	private Long writeCount = 0L;

	@Column(name = "skip_count", nullable = false)
	private Long skipCount = 0L;

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
