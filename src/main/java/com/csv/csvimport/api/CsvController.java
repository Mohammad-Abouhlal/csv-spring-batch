package com.csv.csvimport.api;

import com.csv.csvimport.domain.BatchExecution;
import com.csv.csvimport.domain.BatchExecutionItem;
import com.csv.csvimport.domain.FileProcessingStatus;
import com.csv.csvimport.service.BatchExecutionItemService;
import com.csv.csvimport.service.BatchExecutionService;
import com.csv.csvimport.service.BatchService;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class CsvController {
    public static final String API_BASE_PATH = "/jobs";

    private final BatchService batch;
    private final BatchExecutionService executions;
    private final BatchExecutionItemService items;

    public CsvController(BatchService batch, BatchExecutionService executions, BatchExecutionItemService items) {
        this.batch = batch;
        this.executions = executions;
        this.items = items;
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> importCsv(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        return start("import", file, request);
    }

    @PostMapping(value = "/update", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateCsv(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        return start("update", file, request);
    }

    @PostMapping(value = "/delete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> deleteCsv(@RequestParam("file") MultipartFile file, HttpServletRequest request) throws IOException {
        return start("delete", file, request);
    }

    private ResponseEntity<Void> start(String operation, MultipartFile file, HttpServletRequest request) throws IOException {
        String id = batch.start(operation, file.getBytes());
        return ResponseEntity.created(createResourceURI(id, request)).build();
    }

    protected URI createResourceURI(String batchExecutionId, HttpServletRequest request) {
        return ServletUriComponentsBuilder.fromServletMapping(request)
                .path(BatchInstanceController.API_BASE_PATH).pathSegment(batchExecutionId).build().toUri();
    }

    @GetMapping(API_BASE_PATH + "/{id}")
    public JobStatus status(@PathVariable String id) {
        BatchExecution execution = executions.get(id);
        return new JobStatus(execution.getId(), execution.getOperation(), execution.getStatus(),
                execution.getProcessed(), execution.getErrorMessage());
    }

    @GetMapping(API_BASE_PATH + "/{id}/items")
    public List<BatchExecutionItem> items(@PathVariable String id) {
        executions.get(id);
        return items.items(id);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> missing(NoSuchElementException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    public record JobStatus(String id, String operation, FileProcessingStatus status, long processed, String errorMessage) { }
}


