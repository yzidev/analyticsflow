package com.yzidev.analyticsflow.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yzidev.analyticsflow.dto.request.ImportJobRequest;
import com.yzidev.analyticsflow.dto.response.EtlJobResponse;
import com.yzidev.analyticsflow.dto.response.EtlJobStepResponse;
import com.yzidev.analyticsflow.dto.response.InvalidRecordResponse;
import com.yzidev.analyticsflow.job.service.JobMonitoringService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/jobs")
public class JobController {

	private final JobMonitoringService jobMonitoringService;

	public JobController(JobMonitoringService jobMonitoringService) {
		this.jobMonitoringService = jobMonitoringService;
	}

	@PostMapping("/import")
	EtlJobResponse importCsv(@Valid @RequestBody ImportJobRequest request) {
		return jobMonitoringService.startImport(request);
	}

	@PostMapping("/{jobId}/resume")
	EtlJobResponse resumeImport(
			@PathVariable String jobId,
			@Valid @RequestBody(required = false) ImportJobRequest request) {
		return jobMonitoringService.resumeImport(jobId, request);
	}

	@GetMapping
	List<EtlJobResponse> jobs() {
		return jobMonitoringService.listJobs();
	}

	@GetMapping("/{jobId}")
	EtlJobResponse job(@PathVariable String jobId) {
		return jobMonitoringService.getJob(jobId);
	}

	@GetMapping("/{jobId}/steps")
	List<EtlJobStepResponse> steps(@PathVariable String jobId) {
		return jobMonitoringService.getSteps(jobId);
	}

	@GetMapping("/{jobId}/errors")
	List<InvalidRecordResponse> errors(@PathVariable String jobId) {
		return jobMonitoringService.getErrors(jobId);
	}
}
