package com.yzidev.analyticsflow.batch.staging;

import javax.sql.DataSource;

import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;

import com.yzidev.analyticsflow.common.enums.StagingWriterStrategy;
import com.yzidev.analyticsflow.entity.staging.BaseStagingEntity;

public final class StagingItemWriters {

	private StagingItemWriters() {
	}

	public static <T extends BaseStagingEntity> ItemWriter<T> create(
			String writerStrategy,
			JpaRepository<T, Long> repository,
			JdbcTemplate jdbcTemplate,
			DataSource dataSource,
			StagingEntitySqlMapping<T> mapping) {
		return switch (StagingWriterStrategy.from(writerStrategy)) {
			case JPA -> new JpaRepositoryItemWriter<>(repository, jdbcTemplate, mapping);
			case JDBC -> new JdbcBatchStagingItemWriter<>(jdbcTemplate, mapping);
			case COPY -> new PostgreSqlCopyStagingItemWriter<>(dataSource, mapping);
		};
	}
}
