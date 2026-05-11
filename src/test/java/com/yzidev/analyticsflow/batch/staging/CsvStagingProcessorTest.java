package com.yzidev.analyticsflow.batch.staging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.yzidev.analyticsflow.common.enums.CsvDataset;
import com.yzidev.analyticsflow.entity.staging.UserStagingEntity;
import com.yzidev.analyticsflow.entity.support.InvalidRecordEntity;
import com.yzidev.analyticsflow.repository.jpa.support.InvalidRecordRepository;

class CsvStagingProcessorTest {

	@Test
	void mapsValidRowsIntoStagingEntity() {
		InvalidRecordRepository invalidRecordRepository = mock(InvalidRecordRepository.class);
		var processor = new CsvStagingProcessor<>("job-1", CsvDataset.USERS, this::mapUser, invalidRecordRepository);

		UserStagingEntity entity = processor.process(new CsvRow(CsvDataset.USERS, 2L,
				"u-1,Jane,jane@example.com,123,Jakarta,ID,2026-01-01T00:00:00",
				List.of("u-1", "Jane", "jane@example.com", "123", "Jakarta", "ID", "2026-01-01T00:00:00")));

		assertThat(entity.getJobId()).isEqualTo("job-1");
		assertThat(entity.getRowNumber()).isEqualTo(2L);
		assertThat(entity.getUserId()).isEqualTo("u-1");
		assertThat(entity.getValid()).isTrue();
		assertThat(entity.getErrorMessage()).isNull();
	}

	@Test
	void recordsInvalidRowsWhenColumnCountDoesNotMatch() {
		InvalidRecordRepository invalidRecordRepository = mock(InvalidRecordRepository.class);
		var processor = new CsvStagingProcessor<>("job-1", CsvDataset.USERS, this::mapUser, invalidRecordRepository);

		UserStagingEntity entity = processor.process(new CsvRow(CsvDataset.USERS, 9L, "u-1,Jane", List.of("u-1", "Jane")));

		assertThat(entity.getValid()).isFalse();
		assertThat(entity.getErrorMessage()).contains("Invalid column count");
		verify(invalidRecordRepository).save(any(InvalidRecordEntity.class));
	}

	private UserStagingEntity mapUser(List<String> values) {
		UserStagingEntity entity = new UserStagingEntity();
		if (values.isEmpty()) {
			return entity;
		}
		entity.setUserId(values.get(0));
		entity.setFullName(values.get(1));
		entity.setEmail(values.get(2));
		entity.setPhone(values.get(3));
		entity.setCity(values.get(4));
		entity.setCountry(values.get(5));
		entity.setSourceCreatedAt(values.get(6));
		return entity;
	}
}
