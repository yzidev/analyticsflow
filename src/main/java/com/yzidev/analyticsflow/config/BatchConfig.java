package com.yzidev.analyticsflow.config;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.yzidev.analyticsflow.batch.listener.JobCompletionListener;
import com.yzidev.analyticsflow.batch.listener.StepProgressListener;
import com.yzidev.analyticsflow.batch.tasklet.ValidateRequiredFilesTasklet;
import com.yzidev.analyticsflow.batch.tasklet.migration.MigrateMasterDataTasklet;
import com.yzidev.analyticsflow.batch.tasklet.migration.MigrateOrderDataTasklet;
import com.yzidev.analyticsflow.batch.tasklet.migration.MigrateTransactionAndDeliveryDataTasklet;
import com.yzidev.analyticsflow.batch.tasklet.report.GenerateInitialReportTasklet;
import com.yzidev.analyticsflow.batch.tasklet.transform.TransformToAnalyticalTablesTasklet;
import com.yzidev.analyticsflow.common.enums.EtlStepName;

@Configuration
public class BatchConfig {

	@Bean
	Job analyticsFlowEtlJob(
			JobRepository jobRepository,
			JobCompletionListener jobCompletionListener,
			Step validateRequiredFilesStep,
			Step importUsersToStagingStep,
			Step importProductCategoriesToStagingStep,
			Step importProductsToStagingStep,
			Step importProductDetailsToStagingStep,
			Step importOrdersToStagingStep,
			Step importOrderItemsToStagingStep,
			Step importTransactionsToStagingStep,
			Step importDeliveriesToStagingStep,
			Step migrateMasterDataStep,
			Step migrateOrderDataStep,
			Step migrateTransactionAndDeliveryDataStep,
			Step transformToAnalyticalTablesStep,
			Step generateInitialReportStep) {
		return new JobBuilder("analyticsflow-etl-job", jobRepository)
				.listener(jobCompletionListener)
				.start(validateRequiredFilesStep)
				.next(importUsersToStagingStep)
				.next(importProductCategoriesToStagingStep)
				.next(importProductsToStagingStep)
				.next(importProductDetailsToStagingStep)
				.next(importOrdersToStagingStep)
				.next(importOrderItemsToStagingStep)
				.next(importTransactionsToStagingStep)
				.next(importDeliveriesToStagingStep)
				.next(migrateMasterDataStep)
				.next(migrateOrderDataStep)
				.next(migrateTransactionAndDeliveryDataStep)
				.next(transformToAnalyticalTablesStep)
				.next(generateInitialReportStep)
				.build();
	}

	@Bean
	Step validateRequiredFilesStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			ValidateRequiredFilesTasklet tasklet,
			StepProgressListener listener) {
		return step(jobRepository, transactionManager, listener, EtlStepName.VALIDATE_REQUIRED_FILES.name(), tasklet);
	}

	@Bean
	Step migrateMasterDataStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			StepProgressListener listener, MigrateMasterDataTasklet tasklet) {
		return step(jobRepository, transactionManager, listener, EtlStepName.MIGRATE_MASTER_DATA.name(), tasklet);
	}

	@Bean
	Step migrateOrderDataStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			StepProgressListener listener, MigrateOrderDataTasklet tasklet) {
		return step(jobRepository, transactionManager, listener, EtlStepName.MIGRATE_ORDER_DATA.name(), tasklet);
	}

	@Bean
	Step migrateTransactionAndDeliveryDataStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			StepProgressListener listener, MigrateTransactionAndDeliveryDataTasklet tasklet) {
		return step(jobRepository, transactionManager, listener,
				EtlStepName.MIGRATE_TRANSACTION_AND_DELIVERY_DATA.name(), tasklet);
	}

	@Bean
	Step transformToAnalyticalTablesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			StepProgressListener listener, TransformToAnalyticalTablesTasklet tasklet) {
		return step(jobRepository, transactionManager, listener,
				EtlStepName.TRANSFORM_TO_ANALYTICAL_TABLES.name(), tasklet);
	}

	@Bean
	Step generateInitialReportStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			StepProgressListener listener, GenerateInitialReportTasklet tasklet) {
		return step(jobRepository, transactionManager, listener, EtlStepName.GENERATE_INITIAL_REPORT.name(), tasklet);
	}

	private Step step(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			StepProgressListener listener,
			String stepName,
			Tasklet tasklet) {
		return new StepBuilder(stepName, jobRepository)
				.tasklet(tasklet, transactionManager)
				.listener(listener)
				.build();
	}
}
