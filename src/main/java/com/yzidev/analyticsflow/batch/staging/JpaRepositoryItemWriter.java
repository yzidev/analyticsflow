package com.yzidev.analyticsflow.batch.staging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yzidev.analyticsflow.entity.staging.BaseStagingEntity;

public class JpaRepositoryItemWriter<T extends BaseStagingEntity> implements ItemWriter<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(JpaRepositoryItemWriter.class);
	private static final int DUPLICATE_GUARD_BATCH_SIZE = 10_000;

	private final JpaRepository<T, Long> repository;
	private final JdbcTemplate jdbcTemplate;
	private final StagingEntitySqlMapping<T> mapping;

	public JpaRepositoryItemWriter(JpaRepository<T, Long> repository) {
		this(repository, null, null);
	}

	public JpaRepositoryItemWriter(
			JpaRepository<T, Long> repository,
			JdbcTemplate jdbcTemplate,
			StagingEntitySqlMapping<T> mapping) {
		this.repository = repository;
		this.jdbcTemplate = jdbcTemplate;
		this.mapping = mapping;
	}

	@Override
	public void write(Chunk<? extends T> chunk) {
		List<? extends T> items = chunk.getItems();
		List<? extends T> newRows = newRows(items);
		repository.saveAll(newRows);
		if (mapping != null) {
			StagingWriteProgressLogger.log(LOGGER, "jpa", mapping, items, newRows.size(), items.size() - newRows.size());
		}
	}

	private List<? extends T> newRows(List<? extends T> items) {
		if (jdbcTemplate == null || mapping == null || items.isEmpty()) {
			return items;
		}
		String jobId = items.getFirst().getJobId();
		Set<Long> existingRows = existingRowsForItems(jobId, items);
		if (existingRows.isEmpty()) {
			return items;
		}
		return items.stream()
				.filter(item -> !existingRows.contains(item.getRowNumber()))
				.toList();
	}

	private Set<Long> existingRowsForItems(String jobId, List<? extends T> items) {
		Set<Long> existingRows = new HashSet<>();
		for (int start = 0; start < items.size(); start += DUPLICATE_GUARD_BATCH_SIZE) {
			List<? extends T> batch = items.subList(start, Math.min(start + DUPLICATE_GUARD_BATCH_SIZE, items.size()));
			List<Long> rowNumbers = batch.stream()
					.map(BaseStagingEntity::getRowNumber)
					.toList();
			existingRows.addAll(existingRows(jobId, rowNumbers));
		}
		return existingRows;
	}

	private List<Long> existingRows(String jobId, List<Long> rowNumbers) {
		String placeholders = IntStream.range(0, rowNumbers.size())
				.mapToObj(index -> "?")
				.collect(Collectors.joining(", "));
		String sql = "select row_number from " + mapping.tableName()
				+ " where job_id = ? and row_number in (" + placeholders + ")";
		List<Object> arguments = new ArrayList<>();
		arguments.add(jobId);
		arguments.addAll(rowNumbers);
		return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("row_number"), arguments.toArray());
	}
}
