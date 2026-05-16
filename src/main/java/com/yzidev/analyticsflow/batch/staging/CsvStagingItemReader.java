package com.yzidev.analyticsflow.batch.staging;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yzidev.analyticsflow.common.enums.CsvDataset;

public class CsvStagingItemReader implements ItemStreamReader<CsvRow> {

	private static final Logger LOGGER = LoggerFactory.getLogger(CsvStagingItemReader.class);
	private static final String LINE_NUMBER_KEY = "csv.lineNumber";

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
			long restartLineNumber = executionContext.getLong(LINE_NUMBER_KEY, lineNumber);
			while (lineNumber < restartLineNumber && reader.readLine() != null) {
				lineNumber++;
			}
			LOGGER.info(
					"csv_reader_open file={} table={} restartLine={} currentLine={}",
					dataset.fileName(),
					dataset.stagingTable(),
					restartLineNumber,
					lineNumber);
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
		executionContext.putLong(LINE_NUMBER_KEY, lineNumber);
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
