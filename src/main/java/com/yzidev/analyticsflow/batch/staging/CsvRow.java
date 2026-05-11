package com.yzidev.analyticsflow.batch.staging;

import java.util.List;

import com.yzidev.analyticsflow.common.enums.CsvDataset;

public record CsvRow(
		CsvDataset dataset,
		long rowNumber,
		String rawPayload,
		List<String> values) {
}
