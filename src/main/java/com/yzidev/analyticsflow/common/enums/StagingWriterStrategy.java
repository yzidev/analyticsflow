package com.yzidev.analyticsflow.common.enums;

import java.util.Locale;

public enum StagingWriterStrategy {
	JPA,
	JDBC,
	COPY;

	public static StagingWriterStrategy from(String value) {
		if (value == null || value.isBlank()) {
			return JPA;
		}
		return switch (value.trim().toUpperCase(Locale.ROOT).replace('-', '_')) {
			case "JPA", "JPA_REPOSITORY" -> JPA;
			case "JDBC", "JDBC_BATCH" -> JDBC;
			case "COPY", "POSTGRES_COPY", "POSTGRESQL_COPY" -> COPY;
			default -> throw new IllegalArgumentException(
					"writerStrategy must be one of: jpa, jdbc, copy");
		};
	}
}
