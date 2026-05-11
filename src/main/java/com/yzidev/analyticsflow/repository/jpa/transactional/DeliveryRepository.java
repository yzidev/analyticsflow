package com.yzidev.analyticsflow.repository.jpa.transactional;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.common.enums.DeliveryStatus;
import com.yzidev.analyticsflow.entity.transactional.DeliveryEntity;

public interface DeliveryRepository extends JpaRepository<DeliveryEntity, String> {

	List<DeliveryEntity> findByOrderId(String orderId);

	List<DeliveryEntity> findByDeliveryStatus(DeliveryStatus deliveryStatus);
}
