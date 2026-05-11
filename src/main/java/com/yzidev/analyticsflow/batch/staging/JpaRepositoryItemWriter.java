package com.yzidev.analyticsflow.batch.staging;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.data.jpa.repository.JpaRepository;

public class JpaRepositoryItemWriter<T> implements ItemWriter<T> {

	private final JpaRepository<T, Long> repository;

	public JpaRepositoryItemWriter(JpaRepository<T, Long> repository) {
		this.repository = repository;
	}

	@Override
	public void write(Chunk<? extends T> chunk) {
		repository.saveAll(chunk.getItems());
	}
}
