package com.yzidev.analyticsflow.entity.staging;

import com.yzidev.analyticsflow.config.DbSchemas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = DbSchemas.STAGING, name = "stg_users", indexes = {
		@Index(name = "idx_stg_users_job_id", columnList = "job_id"),
		@Index(name = "idx_stg_users_user_id", columnList = "user_id")
})
public class UserStagingEntity extends BaseStagingEntity {

	@Column(name = "user_id", length = 64)
	private String userId;

	@Column(name = "full_name", length = 255)
	private String fullName;

	@Column(name = "email", length = 255)
	private String email;

	@Column(name = "phone", length = 64)
	private String phone;

	@Column(name = "city", length = 128)
	private String city;

	@Column(name = "country", length = 128)
	private String country;

	@Column(name = "source_created_at", length = 64)
	private String sourceCreatedAt;
}
