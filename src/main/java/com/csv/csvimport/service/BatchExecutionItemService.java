package com.csv.csvimport.service;

import com.csv.csvimport.domain.BatchExecutionItem;
import com.csv.csvimport.domain.Employee;
import com.csv.csvimport.repository.BatchExecutionItemRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BatchExecutionItemService {
    private final BatchExecutionItemRepository repository;

    public BatchExecutionItemService(BatchExecutionItemRepository repository) { this.repository = repository; }

    @Transactional
    public void record(String executionId, Employee employee, String operation) {
        repository.save(new BatchExecutionItem(executionId, employee, operation));
    }

    @Transactional(readOnly = true)
    public List<BatchExecutionItem> items(String executionId) {
        return repository.findByExecutionIdOrderById(executionId);
    }
}


