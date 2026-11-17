package com.csv.csvimport.service;

import com.csv.csvimport.domain.BatchExecution;
import com.csv.csvimport.domain.FileProcessingStatus;
import com.csv.csvimport.repository.BatchExecutionRepository;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchExecutionService {
    private final BatchExecutionRepository repository;

    public BatchExecutionService(BatchExecutionRepository repository) { this.repository = repository; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BatchExecution uploaded(String id, String operation, String jobName, Path file) {
        return repository.save(new BatchExecution(id, operation, jobName, file.toString()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void started(String id, Long springBatchExecutionId) {
        BatchExecution execution = get(id);
        execution.setSpringBatchExecutionId(springBatchExecutionId);
        execution.setStatus(FileProcessingStatus.IN_PROGRESS);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void status(String id, FileProcessingStatus status, String error) {
        BatchExecution execution = get(id);
        execution.setStatus(status);
        execution.setErrorMessage(error);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void progress(String id, long count) {
        BatchExecution execution = get(id);
        execution.setProcessed(count);
        execution.setStatus(FileProcessingStatus.IN_PROGRESS);
    }

    @Transactional(readOnly = true)
    public BatchExecution get(String id) {
        return repository.findById(id).orElseThrow(() -> new NoSuchElementException("Job nicht gefunden: " + id));
    }

    @Transactional(readOnly = true)
    public BatchExecution getWithItems(String id) {
        return repository.findWithItemsById(id)
                .orElseThrow(() -> new NoSuchElementException("Job nicht gefunden: " + id));
    }
}


