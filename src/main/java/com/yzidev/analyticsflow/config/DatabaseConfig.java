package com.yzidev.analyticsflow.config;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;

@Configuration
public class DatabaseConfig {

	@Bean
	@ConditionalOnMissingBean
	DataSource dataSource(AnalyticsFlowProperties properties) {
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setJdbcUrl(properties.db().blocking().url());
		dataSource.setUsername(properties.db().blocking().username());
		dataSource.setPassword(properties.db().blocking().password());
		dataSource.setMaximumPoolSize(64);
		dataSource.setMinimumIdle(8);
		dataSource.setConnectionTimeout(60_000);
		return dataSource;
	}

	@Bean
	@ConditionalOnMissingBean
	JdbcTemplate jdbcTemplate(DataSource dataSource) {
		return new JdbcTemplate(dataSource);
	}

	@Bean
	@Primary
	JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		return new JpaTransactionManager(entityManagerFactory);
	}
}
