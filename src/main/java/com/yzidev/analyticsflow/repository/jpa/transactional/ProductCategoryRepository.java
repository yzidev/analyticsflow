package com.yzidev.analyticsflow.repository.jpa.transactional;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.transactional.ProductCategoryEntity;

public interface ProductCategoryRepository extends JpaRepository<ProductCategoryEntity, String> {

	List<ProductCategoryEntity> findByParentCategoryId(String parentCategoryId);
}
