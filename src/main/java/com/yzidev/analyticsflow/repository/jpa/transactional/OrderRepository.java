package com.yzidev.analyticsflow.repository.jpa.transactional;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.common.enums.OrderStatus;
import com.yzidev.analyticsflow.common.enums.SalesChannel;
import com.yzidev.analyticsflow.entity.transactional.OrderEntity;

public interface OrderRepository extends JpaRepository<OrderEntity, String> {

	List<OrderEntity> findByUserId(String userId);

	List<OrderEntity> findByOrderStatus(OrderStatus orderStatus);

	List<OrderEntity> findByChannel(SalesChannel channel);

	List<OrderEntity> findByOrderDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}
