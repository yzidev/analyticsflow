package com.yzidev.analyticsflow.repository.jpa.transactional;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.transactional.ProductDetailEntity;

public interface ProductDetailRepository extends JpaRepository<ProductDetailEntity, String> {

	List<ProductDetailEntity> findByProductId(String productId);

	Optional<ProductDetailEntity> findBySku(String sku);
}
