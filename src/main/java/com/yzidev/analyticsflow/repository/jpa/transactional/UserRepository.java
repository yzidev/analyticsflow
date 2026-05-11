package com.yzidev.analyticsflow.repository.jpa.transactional;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.transactional.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, String> {

	Optional<UserEntity> findByEmail(String email);

	boolean existsByEmail(String email);
}
