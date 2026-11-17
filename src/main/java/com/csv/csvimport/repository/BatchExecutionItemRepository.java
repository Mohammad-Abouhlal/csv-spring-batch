package com.csv.csvimport.repository;

import com.csv.csvimport.domain.BatchExecutionItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchExecutionItemRepository extends JpaRepository<BatchExecutionItem, Long> {
    List<BatchExecutionItem> findByExecutionIdOrderById(String executionId);
}
