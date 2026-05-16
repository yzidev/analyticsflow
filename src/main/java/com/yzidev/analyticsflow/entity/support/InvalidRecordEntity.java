package com.yzidev.analyticsflow.entity.support;

import java.time.LocalDateTime;

import com.yzidev.analyticsflow.config.DbSchemas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(schema = DbSchemas.SUPPORT, name = "invalid_records", indexes = {
		@Index(name = "idx_invalid_records_job_id", columnList = "job_id"),
		@Index(name = "idx_invalid_records_source_file", columnList = "source_file"),
		@Index(name = "idx_invalid_records_source_table", columnList = "source_table")
})
public class InvalidRecordEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "job_id", nullable = false, length = 64)
	private String jobId;

	@Column(name = "source_file", nullable = false, length = 128)
	private String sourceFile;

	@Column(name = "source_table", nullable = false, length = 128)
	private String sourceTable;

	@Column(name = "row_number")
	private Long rowNumber;

	@Column(name = "raw_payload", columnDefinition = "text")
	private String rawPayload;

	@Column(name = "error_message", nullable = false, columnDefinition = "text")
	private String errorMessage;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();
}
