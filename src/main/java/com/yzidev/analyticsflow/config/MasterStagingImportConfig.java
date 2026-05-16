package com.yzidev.analyticsflow.config;

import java.nio.file.Path;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.yzidev.analyticsflow.batch.listener.StepProgressListener;
import com.yzidev.analyticsflow.batch.staging.CsvRow;
import com.yzidev.analyticsflow.batch.staging.CsvStagingItemReader;
import com.yzidev.analyticsflow.batch.staging.CsvStagingProcessor;
import com.yzidev.analyticsflow.batch.staging.StagingEntitySqlMappings;
import com.yzidev.analyticsflow.batch.staging.StagingItemWriters;
import com.yzidev.analyticsflow.common.enums.CsvDataset;
import com.yzidev.analyticsflow.common.enums.EtlStepName;
import com.yzidev.analyticsflow.entity.staging.BaseStagingEntity;
import com.yzidev.analyticsflow.entity.staging.ProductCategoryStagingEntity;
import com.yzidev.analyticsflow.entity.staging.ProductDetailStagingEntity;
import com.yzidev.analyticsflow.entity.staging.ProductStagingEntity;
import com.yzidev.analyticsflow.entity.staging.UserStagingEntity;
import com.yzidev.analyticsflow.repository.jpa.staging.ProductCategoryStagingRepository;
import com.yzidev.analyticsflow.repository.jpa.staging.ProductDetailStagingRepository;
import com.yzidev.analyticsflow.repository.jpa.staging.ProductStagingRepository;
import com.yzidev.analyticsflow.repository.jpa.staging.UserStagingRepository;
import com.yzidev.analyticsflow.repository.jpa.support.InvalidRecordRepository;

@Configuration
public class MasterStagingImportConfig {

	@Bean
	@JobScope
	Step importUsersToStagingStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			StepProgressListener listener,
			@Value("#{jobParameters['chunkSize']}") Long chunkSize,
			@Qualifier("usersStagingReader") ItemStreamReader<CsvRow> reader,
			@Qualifier("usersStagingProcessor") ItemProcessor<CsvRow, UserStagingEntity> processor,
			@Qualifier("usersStagingWriter") ItemWriter<UserStagingEntity> writer) {
		return importStep(jobRepository, transactionManager, listener, EtlStepName.IMPORT_USERS_TO_STAGING,
				chunkSize, reader, processor, writer);
	}

	@Bean
	@JobScope
	Step importProductCategoriesToStagingStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			StepProgressListener listener,
			@Value("#{jobParameters['chunkSize']}") Long chunkSize,
			@Qualifier("productCategoriesStagingReader") ItemStreamReader<CsvRow> reader,
			@Qualifier("productCategoriesStagingProcessor") ItemProcessor<CsvRow, ProductCategoryStagingEntity> processor,
			@Qualifier("productCategoriesStagingWriter") ItemWriter<ProductCategoryStagingEntity> writer) {
		return importStep(jobRepository, transactionManager, listener,
				EtlStepName.IMPORT_PRODUCT_CATEGORIES_TO_STAGING, chunkSize, reader, processor, writer);
	}

	@Bean
	@JobScope
	Step importProductsToStagingStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			StepProgressListener listener,
			@Value("#{jobParameters['chunkSize']}") Long chunkSize,
			@Qualifier("productsStagingReader") ItemStreamReader<CsvRow> reader,
			@Qualifier("productsStagingProcessor") ItemProcessor<CsvRow, ProductStagingEntity> processor,
			@Qualifier("productsStagingWriter") ItemWriter<ProductStagingEntity> writer) {
		return importStep(jobRepository, transactionManager, listener, EtlStepName.IMPORT_PRODUCTS_TO_STAGING,
				chunkSize, reader, processor, writer);
	}

	@Bean
	@JobScope
	Step importProductDetailsToStagingStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			StepProgressListener listener,
			@Value("#{jobParameters['chunkSize']}") Long chunkSize,
			@Qualifier("productDetailsStagingReader") ItemStreamReader<CsvRow> reader,
			@Qualifier("productDetailsStagingProcessor") ItemProcessor<CsvRow, ProductDetailStagingEntity> processor,
			@Qualifier("productDetailsStagingWriter") ItemWriter<ProductDetailStagingEntity> writer) {
		return importStep(jobRepository, transactionManager, listener,
				EtlStepName.IMPORT_PRODUCT_DETAILS_TO_STAGING, chunkSize, reader, processor, writer);
	}

	@Bean
	@StepScope
	ItemStreamReader<CsvRow> usersStagingReader(
			@Value("#{jobParameters['sampleDirectory']}") String sampleDirectory) {
		return reader(sampleDirectory, CsvDataset.USERS);
	}

	@Bean
	@StepScope
	ItemStreamReader<CsvRow> productCategoriesStagingReader(
			@Value("#{jobParameters['sampleDirectory']}") String sampleDirectory) {
		return reader(sampleDirectory, CsvDataset.PRODUCT_CATEGORIES);
	}

	@Bean
	@StepScope
	ItemStreamReader<CsvRow> productsStagingReader(
			@Value("#{jobParameters['sampleDirectory']}") String sampleDirectory) {
		return reader(sampleDirectory, CsvDataset.PRODUCTS);
	}

	@Bean
	@StepScope
	ItemStreamReader<CsvRow> productDetailsStagingReader(
			@Value("#{jobParameters['sampleDirectory']}") String sampleDirectory) {
		return reader(sampleDirectory, CsvDataset.PRODUCT_DETAILS);
	}

	@Bean
	@StepScope
	ItemProcessor<CsvRow, UserStagingEntity> usersStagingProcessor(
			@Value("#{jobParameters['jobId']}") String jobId,
			InvalidRecordRepository invalidRecordRepository) {
		return new CsvStagingProcessor<>(jobId, CsvDataset.USERS, this::mapUser, invalidRecordRepository);
	}

	@Bean
	@StepScope
	ItemProcessor<CsvRow, ProductCategoryStagingEntity> productCategoriesStagingProcessor(
			@Value("#{jobParameters['jobId']}") String jobId,
			InvalidRecordRepository invalidRecordRepository) {
		return new CsvStagingProcessor<>(jobId, CsvDataset.PRODUCT_CATEGORIES, this::mapProductCategory,
				invalidRecordRepository);
	}

	@Bean
	@StepScope
	ItemProcessor<CsvRow, ProductStagingEntity> productsStagingProcessor(
			@Value("#{jobParameters['jobId']}") String jobId,
			InvalidRecordRepository invalidRecordRepository) {
		return new CsvStagingProcessor<>(jobId, CsvDataset.PRODUCTS, this::mapProduct, invalidRecordRepository);
	}

	@Bean
	@StepScope
	ItemProcessor<CsvRow, ProductDetailStagingEntity> productDetailsStagingProcessor(
			@Value("#{jobParameters['jobId']}") String jobId,
			InvalidRecordRepository invalidRecordRepository) {
		return new CsvStagingProcessor<>(jobId, CsvDataset.PRODUCT_DETAILS, this::mapProductDetail,
				invalidRecordRepository);
	}

	@Bean
	@StepScope
	ItemWriter<UserStagingEntity> usersStagingWriter(
			@Value("#{jobParameters['writerStrategy']}") String writerStrategy,
			UserStagingRepository repository,
			JdbcTemplate jdbcTemplate,
			DataSource dataSource) {
		return StagingItemWriters.create(writerStrategy, repository, jdbcTemplate, dataSource,
				StagingEntitySqlMappings.users());
	}

	@Bean
	@StepScope
	ItemWriter<ProductCategoryStagingEntity> productCategoriesStagingWriter(
			@Value("#{jobParameters['writerStrategy']}") String writerStrategy,
			ProductCategoryStagingRepository repository,
			JdbcTemplate jdbcTemplate,
			DataSource dataSource) {
		return StagingItemWriters.create(writerStrategy, repository, jdbcTemplate, dataSource,
				StagingEntitySqlMappings.productCategories());
	}

	@Bean
	@StepScope
	ItemWriter<ProductStagingEntity> productsStagingWriter(
			@Value("#{jobParameters['writerStrategy']}") String writerStrategy,
			ProductStagingRepository repository,
			JdbcTemplate jdbcTemplate,
			DataSource dataSource) {
		return StagingItemWriters.create(writerStrategy, repository, jdbcTemplate, dataSource,
				StagingEntitySqlMappings.products());
	}

	@Bean
	@StepScope
	ItemWriter<ProductDetailStagingEntity> productDetailsStagingWriter(
			@Value("#{jobParameters['writerStrategy']}") String writerStrategy,
			ProductDetailStagingRepository repository,
			JdbcTemplate jdbcTemplate,
			DataSource dataSource) {
		return StagingItemWriters.create(writerStrategy, repository, jdbcTemplate, dataSource,
				StagingEntitySqlMappings.productDetails());
	}

	private <T extends BaseStagingEntity> Step importStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			StepProgressListener listener,
			EtlStepName stepName,
			Long chunkSize,
			ItemStreamReader<CsvRow> reader,
			ItemProcessor<CsvRow, T> processor,
			ItemWriter<T> writer) {
		return new StepBuilder(stepName.name(), jobRepository)
				.<CsvRow, T>chunk(toChunkSize(chunkSize), transactionManager)
				.reader(reader)
				.processor(processor)
				.writer(writer)
				.faultTolerant()
				.skip(IllegalArgumentException.class)
				.skipLimit(10_000)
				.retry(TransientDataAccessException.class)
				.retryLimit(3)
				.listener(listener)
				.build();
	}

	private int toChunkSize(Long chunkSize) {
		if (chunkSize == null || chunkSize <= 0) {
			return 5_000;
		}
		return Math.toIntExact(chunkSize);
	}

	private CsvStagingItemReader reader(String sampleDirectory, CsvDataset dataset) {
		return new CsvStagingItemReader(Path.of(sampleDirectory), dataset);
	}

	private UserStagingEntity mapUser(List<String> values) {
		UserStagingEntity entity = new UserStagingEntity();
		entity.setUserId(value(values, 0));
		entity.setFullName(value(values, 1));
		entity.setEmail(value(values, 2));
		entity.setPhone(value(values, 3));
		entity.setCity(value(values, 4));
		entity.setCountry(value(values, 5));
		entity.setSourceCreatedAt(value(values, 6));
		return entity;
	}

	private ProductCategoryStagingEntity mapProductCategory(List<String> values) {
		ProductCategoryStagingEntity entity = new ProductCategoryStagingEntity();
		entity.setCategoryId(value(values, 0));
		entity.setCategoryName(value(values, 1));
		entity.setParentCategoryId(value(values, 2));
		entity.setDescription(value(values, 3));
		entity.setSourceCreatedAt(value(values, 4));
		return entity;
	}

	private ProductStagingEntity mapProduct(List<String> values) {
		ProductStagingEntity entity = new ProductStagingEntity();
		entity.setProductId(value(values, 0));
		entity.setCategoryId(value(values, 1));
		entity.setProductName(value(values, 2));
		entity.setBrand(value(values, 3));
		entity.setPrice(value(values, 4));
		entity.setActive(value(values, 5));
		entity.setSourceCreatedAt(value(values, 6));
		return entity;
	}

	private ProductDetailStagingEntity mapProductDetail(List<String> values) {
		ProductDetailStagingEntity entity = new ProductDetailStagingEntity();
		entity.setProductDetailId(value(values, 0));
		entity.setProductId(value(values, 1));
		entity.setSku(value(values, 2));
		entity.setColor(value(values, 3));
		entity.setSize(value(values, 4));
		entity.setWeight(value(values, 5));
		entity.setMaterial(value(values, 6));
		entity.setManufactureDate(value(values, 7));
		entity.setExpiryDate(value(values, 8));
		entity.setSourceCreatedAt(value(values, 9));
		return entity;
	}

	private String value(List<String> values, int index) {
		if (values.size() <= index) {
			return null;
		}
		String value = values.get(index);
		return value == null || value.isBlank() ? null : value;
	}
}
