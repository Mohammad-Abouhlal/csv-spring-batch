package com.csv.csvimport.api;

import com.csv.csvimport.domain.FileProcessingStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BatchExecutionDto(UUID id, String jobName, String operation, FileProcessingStatus status,
        List<BatchItemDto> messages, Instant createdAt, Instant updatedAt,
        long processed, String errorMessage) { }
