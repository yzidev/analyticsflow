package com.yzidev.analyticsflow.repository.blocking;

import com.yzidev.analyticsflow.dto.response.ReportSlice;

public interface BlockingAnalyticsReportRepository {

	ReportSlice salesDailySummary();

	ReportSlice salesProductSummary();

	ReportSlice salesCustomerSummary();

	ReportSlice deliveryPerformanceSummary();

	ReportSlice paymentMethodSummary();

	ReportSlice channelSalesSummary();
}
