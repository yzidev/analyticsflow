package com.yzidev.analyticsflow.ingestion.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.yzidev.analyticsflow.dto.response.FileMetadataResponse;
import com.yzidev.analyticsflow.dto.response.FileValidationResponse;
import com.yzidev.analyticsflow.ingestion.validator.CsvFileValidator;

@Service
public class FileIngestionService {

	private final CsvFileValidator validator;

	public FileIngestionService(CsvFileValidator validator) {
		this.validator = validator;
	}

	public List<FileMetadataResponse> listSampleFiles() {
		return validator.listSampleFiles();
	}

	public List<FileMetadataResponse> listSourceFiles() {
		return validator.listSourceFiles();
	}

	public List<String> requiredFiles() {
		return validator.requiredFiles();
	}

	public FileValidationResponse validate(String sampleDirectory) {
		return validator.validate(sampleDirectory);
	}
}
