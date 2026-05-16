package com.yzidev.analyticsflow.batch.staging;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.postgresql.PGConnection;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yzidev.analyticsflow.entity.staging.BaseStagingEntity;

public class PostgreSqlCopyStagingItemWriter<T extends BaseStagingEntity> implements ItemWriter<T> {

	private static final Logger LOGGER = LoggerFactory.getLogger(PostgreSqlCopyStagingItemWriter.class);

	private final DataSource dataSource;
	private final StagingEntitySqlMapping<T> mapping;
	private final String copySql;
	private final String tempTableName;

	public PostgreSqlCopyStagingItemWriter(DataSource dataSource, StagingEntitySqlMapping<T> mapping) {
		this.dataSource = dataSource;
		this.mapping = mapping;
		this.tempTableName = tempTableName(mapping.tableName());
		this.copySql = copySql(mapping, tempTableName);
	}

	@Override
	public void write(Chunk<? extends T> chunk) throws Exception {
		if (chunk.getItems().isEmpty()) {
			return;
		}

		Connection connection = DataSourceUtils.getConnection(dataSource);
		try {
			createTempTable(connection);
			PGConnection pgConnection = connection.unwrap(PGConnection.class);
			pgConnection.getCopyAPI().copyIn(copySql, new StringReader(csv(chunk)));
			int inserted = mergeTempTable(connection);
			StagingWriteProgressLogger.log(
					LOGGER,
					"copy",
					mapping,
					chunk.getItems(),
					inserted,
					chunk.getItems().size() - inserted);
		}
		catch (Exception exception) {
			throw new DataAccessResourceFailureException("Failed to copy staging rows into PostgreSQL", exception);
		}
		finally {
			DataSourceUtils.releaseConnection(connection, dataSource);
		}
	}

	private String copySql(StagingEntitySqlMapping<T> mapping, String tempTableName) {
		return "copy " + tempTableName
				+ " (" + String.join(", ", mapping.columns()) + ") "
				+ "from stdin with (format csv, null '\\N')";
	}

	private void createTempTable(Connection connection) throws Exception {
		try (Statement statement = connection.createStatement()) {
			statement.execute("drop table if exists " + tempTableName);
			statement.execute("create temporary table " + tempTableName
					+ " (like " + mapping.tableName() + " including defaults) on commit drop");
		}
	}

	private int mergeTempTable(Connection connection) throws Exception {
		String columns = String.join(", ", mapping.columns());
		try (Statement statement = connection.createStatement()) {
			return statement.executeUpdate("insert into " + mapping.tableName() + " (" + columns + ") "
					+ "select " + columns + " from " + tempTableName + " "
					+ "on conflict (job_id, row_number) do nothing");
		}
	}

	private String csv(Chunk<? extends T> chunk) {
		StringBuilder builder = new StringBuilder();
		for (T item : chunk.getItems()) {
			builder.append(mapping.values().stream()
					.map(value -> csvValue(value.apply(item)))
					.collect(Collectors.joining(",")))
					.append('\n');
		}
		return builder.toString();
	}

	private String csvValue(Object value) {
		if (value == null) {
			return "\\N";
		}
		String text = value instanceof LocalDateTime localDateTime
				? localDateTime.toString().replace('T', ' ')
				: value.toString();
		return "\"" + text.replace("\"", "\"\"") + "\"";
	}

	private String tempTableName(String tableName) {
		String tableHash = Integer.toUnsignedString(tableName.hashCode(), 36);
		String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
		return "tmp_copy_" + tableHash + "_" + suffix;
	}
}
