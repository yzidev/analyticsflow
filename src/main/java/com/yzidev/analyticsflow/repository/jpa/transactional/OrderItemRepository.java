package com.yzidev.analyticsflow.repository.jpa.transactional;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.entity.transactional.OrderItemEntity;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, String> {

	List<OrderItemEntity> findByOrderId(String orderId);

	List<OrderItemEntity> findByProductId(String productId);
}
