package com.yzidev.analyticsflow.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "analyticsflow")
public record AnalyticsFlowProperties(
		DataProperties data,
		BatchProperties batch,
		DatabaseProperties db) {

	public record DataProperties(String sourceDirectory, String sampleDirectory, String reportsDirectory) {
	}

	public record BatchProperties(int defaultChunkSize) {
	}

	public record DatabaseProperties(BlockingDatabaseProperties blocking, ReactiveDatabaseProperties reactive) {
	}

	public record BlockingDatabaseProperties(String url, String username, String password) {
	}

	public record ReactiveDatabaseProperties(String url, String username, String password) {
	}
}
