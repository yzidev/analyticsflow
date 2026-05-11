package com.yzidev.analyticsflow.repository.jpa.transactional;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.transactional.ProductEntity;

public interface ProductRepository extends JpaRepository<ProductEntity, String> {

	List<ProductEntity> findByCategoryId(String categoryId);

	List<ProductEntity> findByActiveTrue();
}
