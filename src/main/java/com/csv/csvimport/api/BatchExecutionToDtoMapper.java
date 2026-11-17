package com.csv.csvimport.api;

import com.csv.csvimport.domain.BatchExecution;
import com.csv.csvimport.domain.BatchExecutionItem;
import com.csv.csvimport.domain.BatchExecutionItemMessage;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class BatchExecutionToDtoMapper implements Function<BatchExecution, BatchExecutionDto> {
    @Override
    public BatchExecutionDto apply(BatchExecution execution) {
        return new BatchExecutionDto(UUID.fromString(execution.getId()), execution.getJobName(),
                execution.getOperation(), execution.getStatus(),
                execution.getExecutionItems().stream()
                        .filter(item -> item.getMessage() != null)
                        .sorted(Comparator.comparing(BatchExecutionItem::getId))
                        .map(this::toItemDto).toList(),
                execution.getCreatedAt(), execution.getUpdatedAt(),
                execution.getProcessed(), execution.getErrorMessage());
    }

    private BatchItemDto toItemDto(BatchExecutionItem item) {
        return new BatchItemDto(item.getEmployeeId(), item.getPersonalNumber(), item.getRowNumber(),
                item.getResult(), toItemMessageDto(item.getMessage()));
    }

    private BatchItemMessageDto toItemMessageDto(BatchExecutionItemMessage message) {
        return new BatchItemMessageDto(message.getExceptionName(), message.getSeverityLevel(),
                message.getField(), message.getText(), message.getReason());
    }
}
