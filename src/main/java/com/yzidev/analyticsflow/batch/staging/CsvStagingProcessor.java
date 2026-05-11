package com.yzidev.analyticsflow.batch.staging;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.yzidev.analyticsflow.common.enums.CsvDataset;
import com.yzidev.analyticsflow.entity.staging.BaseStagingEntity;
import com.yzidev.analyticsflow.entity.support.InvalidRecordEntity;
import com.yzidev.analyticsflow.repository.jpa.support.InvalidRecordRepository;

public class CsvStagingProcessor<T extends BaseStagingEntity> implements ItemProcessor<CsvRow, T> {

	private final String jobId;
	private final CsvDataset dataset;
	private final StagingEntityMapper<T> mapper;
	private final InvalidRecordRepository invalidRecordRepository;

	public CsvStagingProcessor(
			String jobId,
			CsvDataset dataset,
			StagingEntityMapper<T> mapper,
			InvalidRecordRepository invalidRecordRepository) {
		this.jobId = jobId;
		this.dataset = dataset;
		this.mapper = mapper;
		this.invalidRecordRepository = invalidRecordRepository;
	}

	@Override
	public T process(CsvRow row) {
		T entity;
		String errorMessage = null;

		try {
			validateColumnCount(row.values());
			entity = mapper.map(row.values());
		}
		catch (RuntimeException exception) {
			entity = mapper.map(List.of());
			errorMessage = exception.getMessage();
		}

		entity.setJobId(jobId);
		entity.setRowNumber(row.rowNumber());
		entity.setRawPayload(row.rawPayload());
		entity.setValid(errorMessage == null);
		entity.setErrorMessage(errorMessage);
		entity.setCreatedAt(LocalDateTime.now());

		if (errorMessage != null) {
			invalidRecordRepository.save(toInvalidRecord(row, errorMessage));
		}

		return entity;
	}

	private void validateColumnCount(List<String> values) {
		int expected = dataset.expectedHeader().size();
		if (values.size() != expected) {
			throw new IllegalArgumentException(
					"Invalid column count. Expected " + expected + " but got " + values.size());
		}
	}

	private InvalidRecordEntity toInvalidRecord(CsvRow row, String errorMessage) {
		InvalidRecordEntity invalidRecord = new InvalidRecordEntity();
		invalidRecord.setJobId(jobId);
		invalidRecord.setSourceFile(row.dataset().fileName());
		invalidRecord.setSourceTable(row.dataset().stagingTable());
		invalidRecord.setRowNumber(row.rowNumber());
		invalidRecord.setRawPayload(row.rawPayload());
		invalidRecord.setErrorMessage(errorMessage);
		invalidRecord.setCreatedAt(LocalDateTime.now());
		return invalidRecord;
	}
}
