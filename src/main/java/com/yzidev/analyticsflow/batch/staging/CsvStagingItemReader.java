package com.yzidev.analyticsflow.batch.staging;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

import com.yzidev.analyticsflow.common.enums.CsvDataset;

public class CsvStagingItemReader implements ItemStreamReader<CsvRow> {

	private final Path filePath;
	private final CsvDataset dataset;
	private BufferedReader reader;
	private long lineNumber;

	public CsvStagingItemReader(Path directory, CsvDataset dataset) {
		this.filePath = directory.resolve(dataset.fileName()).normalize();
		this.dataset = dataset;
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		try {
			reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8);
			reader.readLine();
			lineNumber = 1L;
		}
		catch (IOException exception) {
			throw new ItemStreamException("Failed to open CSV file: " + filePath, exception);
		}
	}

	@Override
	public CsvRow read() throws Exception {
		String line = reader.readLine();
		if (line == null) {
			return null;
		}
		lineNumber++;
		return new CsvRow(dataset, lineNumber, line, CsvLineParser.parse(line));
	}

	@Override
	public void update(ExecutionContext executionContext) {
	}

	@Override
	public void close() throws ItemStreamException {
		if (reader == null) {
			return;
		}
		try {
			reader.close();
		}
		catch (IOException exception) {
			throw new ItemStreamException("Failed to close CSV file: " + filePath, exception);
		}
	}
}
