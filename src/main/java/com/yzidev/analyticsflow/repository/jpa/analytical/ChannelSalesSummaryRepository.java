package com.yzidev.analyticsflow.repository.jpa.analytical;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.yzidev.analyticsflow.common.enums.SalesChannel;
import com.yzidev.analyticsflow.entity.analytical.ChannelSalesSummaryEntity;

public interface ChannelSalesSummaryRepository extends JpaRepository<ChannelSalesSummaryEntity, Long> {

	List<ChannelSalesSummaryEntity> findBySummaryDateBetween(LocalDate startDate, LocalDate endDate);

	List<ChannelSalesSummaryEntity> findByChannel(SalesChannel channel);
}
