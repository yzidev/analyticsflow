package com.yzidev.analyticsflow.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yzidev.analyticsflow.dto.request.FileValidationRequest;
import com.yzidev.analyticsflow.dto.response.FileMetadataResponse;
import com.yzidev.analyticsflow.dto.response.FileValidationResponse;
import com.yzidev.analyticsflow.dto.response.RequiredFilesResponse;
import com.yzidev.analyticsflow.ingestion.service.FileIngestionService;

@RestController
@RequestMapping("/api/files")
public class FileController {

	private final FileIngestionService fileIngestionService;

	public FileController(FileIngestionService fileIngestionService) {
		this.fileIngestionService = fileIngestionService;
	}

	@GetMapping("/sample")
	List<FileMetadataResponse> sampleFiles() {
		return fileIngestionService.listSampleFiles();
	}

	@GetMapping("/source")
	List<FileMetadataResponse> sourceFiles() {
		return fileIngestionService.listSourceFiles();
	}

	@GetMapping("/required")
	RequiredFilesResponse requiredFiles() {
		return new RequiredFilesResponse(fileIngestionService.requiredFiles());
	}

	@PostMapping("/validate")
	FileValidationResponse validate(@RequestBody(required = false) FileValidationRequest request) {
		String sampleDirectory = request == null ? null : request.sampleDirectory();
		return fileIngestionService.validate(sampleDirectory);
	}
}
