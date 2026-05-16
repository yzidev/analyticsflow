package com.yzidev.analyticsflow.entity.transactional;

import java.time.LocalDateTime;

import com.yzidev.analyticsflow.config.DbSchemas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(schema = DbSchemas.OLTP, name = "users", indexes = {
		@Index(name = "idx_users_user_id", columnList = "user_id"),
		@Index(name = "idx_users_email", columnList = "email")
})
public class UserEntity {

	@Id
	@Column(name = "user_id", nullable = false, length = 64)
	private String userId;

	@Column(name = "full_name", nullable = false, length = 255)
	private String fullName;

	@Column(name = "email", length = 255)
	private String email;

	@Column(name = "phone", length = 64)
	private String phone;

	@Column(name = "city", length = 128)
	private String city;

	@Column(name = "country", length = 128)
	private String country;

	@Column(name = "created_at")
	private LocalDateTime createdAt;
}
