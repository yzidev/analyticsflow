package com.yzidev.analyticsflow.reporting.service;

import com.yzidev.analyticsflow.dto.response.ReportSlice;

record ReportQueryResult(ReportSlice slice, String threadName) {
}
