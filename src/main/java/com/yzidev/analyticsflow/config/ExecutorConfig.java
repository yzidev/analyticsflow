package com.yzidev.analyticsflow.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ExecutorConfig {

	@Bean(name = "analyticsTaskExecutor")
	Executor analyticsTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("analyticsflow-async-");
		executor.setCorePoolSize(8);
		executor.setMaxPoolSize(32);
		executor.setQueueCapacity(256);
		executor.initialize();
		return executor;
	}

	@Bean(name = "analyticsCompletableFutureExecutor")
	Executor analyticsCompletableFutureExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setThreadNamePrefix("analyticsflow-cf-");
		executor.setCorePoolSize(16);
		executor.setMaxPoolSize(64);
		executor.setQueueCapacity(1024);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.initialize();
		return executor;
	}

	@Bean(name = "analyticsVirtualThreadExecutor", destroyMethod = "close")
	AutoCloseableExecutorService analyticsVirtualThreadExecutor() {
		return new AutoCloseableExecutorService(Executors.newVirtualThreadPerTaskExecutor());
	}
}
