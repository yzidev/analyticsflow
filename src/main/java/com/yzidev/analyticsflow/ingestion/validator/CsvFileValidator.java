package com.yzidev.analyticsflow.ingestion.validator;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.yzidev.analyticsflow.common.enums.CsvDataset;
import com.yzidev.analyticsflow.config.AnalyticsFlowProperties;
import com.yzidev.analyticsflow.dto.response.FileMetadataResponse;
import com.yzidev.analyticsflow.dto.response.FileValidationResponse;

@Component
public class CsvFileValidator {

	private final AnalyticsFlowProperties properties;

	public CsvFileValidator(AnalyticsFlowProperties properties) {
		this.properties = properties;
	}

	public List<String> requiredFiles() {
		return Arrays.stream(CsvDataset.values())
				.map(CsvDataset::fileName)
				.toList();
	}

	public List<FileMetadataResponse> listSampleFiles() {
		return listFiles(Path.of(properties.data().sampleDirectory()).toAbsolutePath().normalize());
	}

	public List<FileMetadataResponse> listSourceFiles() {
		return listFiles(sourceRoot());
	}

	private List<FileMetadataResponse> listFiles(Path directory) {
		Path sampleDirectory = directory;
		if (!Files.isDirectory(sampleDirectory)) {
			return List.of();
		}
		try (var stream = Files.list(sampleDirectory)) {
			return stream
					.filter(this::isVisibleRegularFile)
					.sorted(Comparator.comparing(path -> path.getFileName().toString()))
					.map(path -> fileMetadata(path, expectedHeader(path.getFileName().toString())))
					.toList();
		}
		catch (IOException exception) {
			return List.of();
		}
	}

	public FileValidationResponse validate(String requestedDirectory) {
		Path root = sourceRoot();
		Path sampleRoot = sampleRoot();
		Path target = resolveRequestedDirectory(requestedDirectory);
		List<String> errors = new ArrayList<>();
		List<FileMetadataResponse> files = new ArrayList<>();
		List<String> missingFiles = new ArrayList<>();
		List<String> invalidHeaders = new ArrayList<>();

		if (!target.equals(root) && !target.equals(sampleRoot)) {
			errors.add("sampleDirectory must resolve to " + properties.data().sourceDirectory()
					+ " for real imports or " + properties.data().sampleDirectory() + " for testing");
		}
		if (!Files.exists(target)) {
			errors.add("sampleDirectory does not exist: " + target);
		}
		if (Files.exists(target) && !Files.isDirectory(target)) {
			errors.add("sampleDirectory is not a directory: " + target);
		}

		if (errors.isEmpty()) {
			for (CsvDataset dataset : CsvDataset.values()) {
				Path file = target.resolve(dataset.fileName()).normalize();
				if (!file.startsWith(target)) {
					errors.add("Rejected path outside requested data directory: " + dataset.fileName());
					continue;
				}
				if (!Files.exists(file)) {
					missingFiles.add(dataset.fileName());
					continue;
				}
				FileMetadataResponse metadata = fileMetadata(file, dataset.expectedHeader());
				files.add(metadata);
				if (!metadata.headerValid()) {
					invalidHeaders.add(dataset.fileName());
				}
			}
		}

		List<String> availableFiles = listAvailableFileNames(target);
		boolean valid = errors.isEmpty() && missingFiles.isEmpty() && invalidHeaders.isEmpty();
		return new FileValidationResponse(valid, missingFiles, invalidHeaders, availableFiles, files, errors);
	}

	private List<String> listAvailableFileNames(Path directory) {
		if (!Files.isDirectory(directory)) {
			return List.of();
		}
		try (var stream = Files.list(directory)) {
			return stream
					.filter(this::isVisibleRegularFile)
					.map(path -> path.getFileName().toString())
					.sorted()
					.toList();
		}
		catch (IOException exception) {
			return List.of();
		}
	}

	private Path resolveRequestedDirectory(String requestedDirectory) {
		if (requestedDirectory == null || requestedDirectory.isBlank()) {
			return sourceRoot();
		}
		return Path.of(requestedDirectory).toAbsolutePath().normalize();
	}

	private Path sourceRoot() {
		return Path.of(properties.data().sourceDirectory()).toAbsolutePath().normalize();
	}

	private Path sampleRoot() {
		return Path.of(properties.data().sampleDirectory()).toAbsolutePath().normalize();
	}

	private boolean isVisibleRegularFile(Path path) {
		return Files.isRegularFile(path) && !path.getFileName().toString().startsWith(".");
	}

	private List<String> expectedHeader(String fileName) {
		return Arrays.stream(CsvDataset.values())
				.filter(dataset -> dataset.fileName().equals(fileName))
				.findFirst()
				.map(CsvDataset::expectedHeader)
				.orElse(List.of());
	}

	private FileMetadataResponse fileMetadata(Path file, List<String> expectedHeader) {
		List<String> errors = new ArrayList<>();
		boolean readable = Files.isReadable(file);
		boolean csvExtension = file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv");
		if (!readable) {
			errors.add("File is not readable");
		}
		if (!csvExtension) {
			errors.add("File extension must be .csv");
		}

		List<String> actualHeader = readHeader(file, errors);
		boolean headerValid = !expectedHeader.isEmpty() && expectedHeader.equals(actualHeader);
		if (!expectedHeader.isEmpty() && !headerValid) {
			errors.add("Header does not match expected columns: " + String.join(",", expectedHeader));
		}

		return new FileMetadataResponse(
				file.getFileName().toString(),
				file.toAbsolutePath().normalize().toString(),
				size(file),
				lastModified(file),
				readable,
				csvExtension,
				actualHeader,
				headerValid,
				errors);
	}

	private List<String> readHeader(Path file, List<String> errors) {
		try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			String header = reader.readLine();
			if (header == null || header.isBlank()) {
				errors.add("Header row is empty");
				return List.of();
			}
			return Arrays.stream(header.split(",", -1))
					.map(String::trim)
					.toList();
		}
		catch (IOException exception) {
			errors.add("Cannot read header: " + exception.getMessage());
			return List.of();
		}
	}

	private long size(Path file) {
		try {
			return Files.size(file);
		}
		catch (IOException exception) {
			return 0;
		}
	}

	private Instant lastModified(Path file) {
		try {
			return Files.getLastModifiedTime(file).toInstant();
		}
		catch (IOException exception) {
			return null;
		}
	}
}
