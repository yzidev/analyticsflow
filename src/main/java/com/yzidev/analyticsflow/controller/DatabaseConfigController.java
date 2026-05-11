package com.yzidev.analyticsflow.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yzidev.analyticsflow.config.AnalyticsFlowProperties;

@RestController
@RequestMapping("/api/config/database")
public class DatabaseConfigController {

	private final AnalyticsFlowProperties properties;

	public DatabaseConfigController(AnalyticsFlowProperties properties) {
		this.properties = properties;
	}

	@GetMapping
	AnalyticsFlowProperties.DatabaseProperties databaseConfig() {
		return properties.db();
	}
}
