package com.yzidev.analyticsflow.entity.staging;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@MappedSuperclass
public abstract class BaseStagingEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "job_id", nullable = false, length = 64)
	private String jobId;

	@Column(name = "row_number", nullable = false)
	private Long rowNumber;

	@Column(name = "raw_payload", columnDefinition = "text")
	private String rawPayload;

	@Column(name = "is_valid", nullable = false)
	private Boolean valid = Boolean.TRUE;

	@Column(name = "error_message", columnDefinition = "text")
	private String errorMessage;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
}
