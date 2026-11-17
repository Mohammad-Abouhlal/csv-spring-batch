package com.csv.csvimport.api;

import com.csv.csvimport.domain.BatchExecutionItem;
import com.csv.csvimport.service.BatchExecutionItemService;
import com.csv.csvimport.service.BatchExecutionService;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping(BatchInstanceController.API_BASE_PATH)
public class BatchInstanceController {
    public static final String API_BASE_PATH = "/api/batch-instance";
    private final BatchExecutionService executions;
    private final BatchExecutionItemService items;
    private final BatchExecutionToDtoMapper mapper;

    public BatchInstanceController(BatchExecutionService executions, BatchExecutionItemService items,
            BatchExecutionToDtoMapper mapper) {
        this.executions = executions;
        this.items = items;
        this.mapper = mapper;
    }

    @GetMapping("/{id}")
    public ResponseEntity<BatchExecutionDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(mapper.apply(executions.getWithItems(id.toString())));
    }

    @GetMapping("/{id}/items")
    public List<BatchExecutionItem> items(@PathVariable UUID id) {
        executions.get(id.toString());
        return items.items(id.toString());
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Void> missing() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}

