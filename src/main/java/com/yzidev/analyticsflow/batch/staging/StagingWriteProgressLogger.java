package com.yzidev.analyticsflow.batch.staging;

import java.util.List;

import org.slf4j.Logger;

import com.yzidev.analyticsflow.entity.staging.BaseStagingEntity;

final class StagingWriteProgressLogger {

	private StagingWriteProgressLogger() {
	}

	static <T extends BaseStagingEntity> void log(
			Logger logger,
			String strategy,
			StagingEntitySqlMapping<T> mapping,
			List<? extends T> items,
			int inserted,
			int skipped) {
		if (items.isEmpty()) {
			return;
		}
		long fromRow = items.stream().mapToLong(BaseStagingEntity::getRowNumber).min().orElse(0L);
		long toRow = items.stream().mapToLong(BaseStagingEntity::getRowNumber).max().orElse(0L);
		logger.info(
				"staging_import_progress jobId={} file={} table={} strategy={} rowRange={}-{} attempted={} inserted={} skipped={}",
				items.getFirst().getJobId(),
				mapping.sourceFile(),
				mapping.tableName(),
				strategy,
				fromRow,
				toRow,
				items.size(),
				inserted,
				skipped);
	}
}
