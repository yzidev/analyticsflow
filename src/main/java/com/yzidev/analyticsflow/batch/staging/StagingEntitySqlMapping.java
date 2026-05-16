package com.yzidev.analyticsflow.batch.staging;

import java.util.List;
import java.util.function.Function;

import com.yzidev.analyticsflow.entity.staging.BaseStagingEntity;

public record StagingEntitySqlMapping<T extends BaseStagingEntity>(
		String sourceFile,
		String tableName,
		List<String> columns,
		List<Function<T, Object>> values) {

	public StagingEntitySqlMapping {
		if (columns.size() != values.size()) {
			throw new IllegalArgumentException("columns and values must have the same size");
		}
	}
}
