package com.csv.csvimport.service;

import com.csv.csvimport.domain.BatchExecutionItem;
import com.csv.csvimport.domain.Employee;
import com.csv.csvimport.repository.BatchExecutionItemRepository;
import java.util.List;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchExecutionItemService {
    private final BatchExecutionItemRepository repository;

    public BatchExecutionItemService(BatchExecutionItemRepository repository) { this.repository = repository; }

    @Transactional
    public void record(String executionId, Employee employee, String operation) {
        repository.save(new BatchExecutionItem(executionId, employee, operation));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordError(String executionId, CSVRecord record, Throwable error) {
        String rawId = record.toMap().get("id");
        Long employeeId = null;
        try { employeeId = rawId == null ? null : Long.valueOf(rawId); }
        catch (NumberFormatException ignored) { /* Ungültige IDs bleiben als Zeilenfehler sichtbar. */ }
        String number = record.toMap().get("personalNumber");
        if (number != null && number.length() > 255) number = number.substring(0, 255);
        repository.save(new BatchExecutionItem(executionId, employeeId, number,
                record.getRecordNumber(), error));
    }

    @Transactional
    public void clearSuccessful(String executionId) {
        repository.deleteByExecutionIdAndResultNot(executionId, "ERROR");
    }

    @Transactional(readOnly = true)
    public List<BatchExecutionItem> items(String executionId) {
        return repository.findByExecutionIdOrderById(executionId);
    }
}


