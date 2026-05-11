package com.yzidev.analyticsflow.config;

import java.nio.file.Path;
import java.util.List;

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
import org.springframework.transaction.PlatformTransactionManager;

import com.yzidev.analyticsflow.batch.listener.StepProgressListener;
import com.yzidev.analyticsflow.batch.staging.CsvRow;
import com.yzidev.analyticsflow.batch.staging.CsvStagingItemReader;
import com.yzidev.analyticsflow.batch.staging.CsvStagingProcessor;
import com.yzidev.analyticsflow.batch.staging.JpaRepositoryItemWriter;
import com.yzidev.analyticsflow.common.enums.CsvDataset;
import com.yzidev.analyticsflow.common.enums.EtlStepName;
import com.yzidev.analyticsflow.entity.staging.BaseStagingEntity;
import com.yzidev.analyticsflow.entity.staging.DeliveryStagingEntity;
import com.yzidev.analyticsflow.entity.staging.OrderItemStagingEntity;
import com.yzidev.analyticsflow.entity.staging.OrderStagingEntity;
import com.yzidev.analyticsflow.entity.staging.TransactionStagingEntity;
import com.yzidev.analyticsflow.repository.jpa.staging.DeliveryStagingRepository;
import com.yzidev.analyticsflow.repository.jpa.staging.OrderItemStagingRepository;
import com.yzidev.analyticsflow.repository.jpa.staging.OrderStagingRepository;
import com.yzidev.analyticsflow.repository.jpa.staging.TransactionStagingRepository;
import com.yzidev.analyticsflow.repository.jpa.support.InvalidRecordRepository;

@Configuration
public class TransactionStagingImportConfig {

	@Bean
	@JobScope
	Step importOrdersToStagingStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			StepProgressListener listener,
			@Value("#{jobParameters['chunkSize']}") Long chunkSize,
			@Qualifier("ordersStagingReader") ItemStreamReader<CsvRow> reader,
			@Qualifier("ordersStagingProcessor") ItemProcessor<CsvRow, OrderStagingEntity> processor,
			@Qualifier("ordersStagingWriter") ItemWriter<OrderStagingEntity> writer) {
		return importStep(jobRepository, transactionManager, listener, EtlStepName.IMPORT_ORDERS_TO_STAGING,
				chunkSize, reader, processor, writer);
	}

	@Bean
	@JobScope
	Step importOrderItemsToStagingStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			StepProgressListener listener,
			@Value("#{jobParameters['chunkSize']}") Long chunkSize,
			@Qualifier("orderItemsStagingReader") ItemStreamReader<CsvRow> reader,
			@Qualifier("orderItemsStagingProcessor") ItemProcessor<CsvRow, OrderItemStagingEntity> processor,
			@Qualifier("orderItemsStagingWriter") ItemWriter<OrderItemStagingEntity> writer) {
		return importStep(jobRepository, transactionManager, listener, EtlStepName.IMPORT_ORDER_ITEMS_TO_STAGING,
				chunkSize, reader, processor, writer);
	}

	@Bean
	@JobScope
	Step importTransactionsToStagingStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			StepProgressListener listener,
			@Value("#{jobParameters['chunkSize']}") Long chunkSize,
			@Qualifier("transactionsStagingReader") ItemStreamReader<CsvRow> reader,
			@Qualifier("transactionsStagingProcessor") ItemProcessor<CsvRow, TransactionStagingEntity> processor,
			@Qualifier("transactionsStagingWriter") ItemWriter<TransactionStagingEntity> writer) {
		return importStep(jobRepository, transactionManager, listener, EtlStepName.IMPORT_TRANSACTIONS_TO_STAGING,
				chunkSize, reader, processor, writer);
	}

	@Bean
	@JobScope
	Step importDeliveriesToStagingStep(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager,
			StepProgressListener listener,
			@Value("#{jobParameters['chunkSize']}") Long chunkSize,
			@Qualifier("deliveriesStagingReader") ItemStreamReader<CsvRow> reader,
			@Qualifier("deliveriesStagingProcessor") ItemProcessor<CsvRow, DeliveryStagingEntity> processor,
			@Qualifier("deliveriesStagingWriter") ItemWriter<DeliveryStagingEntity> writer) {
		return importStep(jobRepository, transactionManager, listener, EtlStepName.IMPORT_DELIVERIES_TO_STAGING,
				chunkSize, reader, processor, writer);
	}

	@Bean
	@StepScope
	ItemStreamReader<CsvRow> ordersStagingReader(
			@Value("#{jobParameters['sampleDirectory']}") String sampleDirectory) {
		return reader(sampleDirectory, CsvDataset.ORDERS);
	}

	@Bean
	@StepScope
	ItemStreamReader<CsvRow> orderItemsStagingReader(
			@Value("#{jobParameters['sampleDirectory']}") String sampleDirectory) {
		return reader(sampleDirectory, CsvDataset.ORDER_ITEMS);
	}

	@Bean
	@StepScope
	ItemStreamReader<CsvRow> transactionsStagingReader(
			@Value("#{jobParameters['sampleDirectory']}") String sampleDirectory) {
		return reader(sampleDirectory, CsvDataset.TRANSACTIONS);
	}

	@Bean
	@StepScope
	ItemStreamReader<CsvRow> deliveriesStagingReader(
			@Value("#{jobParameters['sampleDirectory']}") String sampleDirectory) {
		return reader(sampleDirectory, CsvDataset.DELIVERIES);
	}

	@Bean
	@StepScope
	ItemProcessor<CsvRow, OrderStagingEntity> ordersStagingProcessor(
			@Value("#{jobParameters['jobId']}") String jobId,
			InvalidRecordRepository invalidRecordRepository) {
		return new CsvStagingProcessor<>(jobId, CsvDataset.ORDERS, this::mapOrder, invalidRecordRepository);
	}

	@Bean
	@StepScope
	ItemProcessor<CsvRow, OrderItemStagingEntity> orderItemsStagingProcessor(
			@Value("#{jobParameters['jobId']}") String jobId,
			InvalidRecordRepository invalidRecordRepository) {
		return new CsvStagingProcessor<>(jobId, CsvDataset.ORDER_ITEMS, this::mapOrderItem,
				invalidRecordRepository);
	}

	@Bean
	@StepScope
	ItemProcessor<CsvRow, TransactionStagingEntity> transactionsStagingProcessor(
			@Value("#{jobParameters['jobId']}") String jobId,
			InvalidRecordRepository invalidRecordRepository) {
		return new CsvStagingProcessor<>(jobId, CsvDataset.TRANSACTIONS, this::mapTransaction,
				invalidRecordRepository);
	}

	@Bean
	@StepScope
	ItemProcessor<CsvRow, DeliveryStagingEntity> deliveriesStagingProcessor(
			@Value("#{jobParameters['jobId']}") String jobId,
			InvalidRecordRepository invalidRecordRepository) {
		return new CsvStagingProcessor<>(jobId, CsvDataset.DELIVERIES, this::mapDelivery,
				invalidRecordRepository);
	}

	@Bean
	ItemWriter<OrderStagingEntity> ordersStagingWriter(OrderStagingRepository repository) {
		return new JpaRepositoryItemWriter<>(repository);
	}

	@Bean
	ItemWriter<OrderItemStagingEntity> orderItemsStagingWriter(OrderItemStagingRepository repository) {
		return new JpaRepositoryItemWriter<>(repository);
	}

	@Bean
	ItemWriter<TransactionStagingEntity> transactionsStagingWriter(TransactionStagingRepository repository) {
		return new JpaRepositoryItemWriter<>(repository);
	}

	@Bean
	ItemWriter<DeliveryStagingEntity> deliveriesStagingWriter(DeliveryStagingRepository repository) {
		return new JpaRepositoryItemWriter<>(repository);
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

	private OrderStagingEntity mapOrder(List<String> values) {
		OrderStagingEntity entity = new OrderStagingEntity();
		entity.setOrderId(value(values, 0));
		entity.setUserId(value(values, 1));
		entity.setOrderDate(value(values, 2));
		entity.setOrderStatus(value(values, 3));
		entity.setTotalAmount(value(values, 4));
		entity.setChannel(value(values, 5));
		entity.setSourceCreatedAt(value(values, 6));
		return entity;
	}

	private OrderItemStagingEntity mapOrderItem(List<String> values) {
		OrderItemStagingEntity entity = new OrderItemStagingEntity();
		entity.setOrderItemId(value(values, 0));
		entity.setOrderId(value(values, 1));
		entity.setProductId(value(values, 2));
		entity.setQuantity(value(values, 3));
		entity.setUnitPrice(value(values, 4));
		entity.setTotalPrice(value(values, 5));
		return entity;
	}

	private TransactionStagingEntity mapTransaction(List<String> values) {
		TransactionStagingEntity entity = new TransactionStagingEntity();
		entity.setTransactionId(value(values, 0));
		entity.setOrderId(value(values, 1));
		entity.setUserId(value(values, 2));
		entity.setTransactionDate(value(values, 3));
		entity.setPaymentMethod(value(values, 4));
		entity.setAmount(value(values, 5));
		entity.setCurrency(value(values, 6));
		entity.setStatus(value(values, 7));
		entity.setSourceCreatedAt(value(values, 8));
		return entity;
	}

	private DeliveryStagingEntity mapDelivery(List<String> values) {
		DeliveryStagingEntity entity = new DeliveryStagingEntity();
		entity.setDeliveryId(value(values, 0));
		entity.setOrderId(value(values, 1));
		entity.setDeliveryStatus(value(values, 2));
		entity.setDeliveryAddress(value(values, 3));
		entity.setShippedDate(value(values, 4));
		entity.setDeliveredDate(value(values, 5));
		entity.setCourierName(value(values, 6));
		entity.setSourceCreatedAt(value(values, 7));
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
