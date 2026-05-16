package com.yzidev.analyticsflow.batch.staging;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yzidev.analyticsflow.entity.staging.BaseStagingEntity;

public class JdbcBatchStagingItemWriter<T extends BaseStagingEntity> implements ItemWriter<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(JdbcBatchStagingItemWriter.class);

	private final JdbcTemplate jdbcTemplate;
	private final StagingEntitySqlMapping<T> mapping;
	private final String insertSql;

	public JdbcBatchStagingItemWriter(JdbcTemplate jdbcTemplate, StagingEntitySqlMapping<T> mapping) {
		this.jdbcTemplate = jdbcTemplate;
		this.mapping = mapping;
		this.insertSql = insertSql(mapping);
	}

	@Override
	public void write(Chunk<? extends T> chunk) {
		List<? extends T> items = chunk.getItems();
		int[] results = jdbcTemplate.batchUpdate(insertSql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				T item = items.get(i);
				for (int column = 0; column < mapping.values().size(); column++) {
					ps.setObject(column + 1, mapping.values().get(column).apply(item));
				}
			}

			@Override
			public int getBatchSize() {
				return items.size();
			}
		});
		int inserted = insertedCount(results, items.size());
		StagingWriteProgressLogger.log(LOGGER, "jdbc", mapping, items, inserted, items.size() - inserted);
	}

	private int insertedCount(int[] results, int attempted) {
		int inserted = 0;
		for (int result : results) {
			if (result > 0) {
				inserted += result;
			}
			else if (result == Statement.SUCCESS_NO_INFO) {
				return attempted;
			}
		}
		return inserted;
	}

	private String insertSql(StagingEntitySqlMapping<T> mapping) {
		String columns = String.join(", ", mapping.columns());
		String placeholders = IntStream.range(0, mapping.columns().size())
				.mapToObj(index -> "?")
				.collect(Collectors.joining(", "));
		return "insert into " + mapping.tableName() + " (" + columns + ") values (" + placeholders + ")"
				+ " on conflict (job_id, row_number) do nothing";
	}
}
