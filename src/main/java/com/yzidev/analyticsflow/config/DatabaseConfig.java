package com.yzidev.analyticsflow.config;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseConfig {

	@Bean
	@ConditionalOnMissingBean
	DataSource dataSource(AnalyticsFlowProperties properties) {
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setJdbcUrl(properties.db().blocking().url());
		dataSource.setUsername(properties.db().blocking().username());
		dataSource.setPassword(properties.db().blocking().password());
		return dataSource;
	}

	@Bean
	@ConditionalOnMissingBean
	JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}
}
