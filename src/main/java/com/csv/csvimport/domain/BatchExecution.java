package com.csv.csvimport.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
public class BatchExecution {
    @Id
    private String id;
    private String operation;
    private String filePath;
    private Long springBatchExecutionId;
    @Enumerated(EnumType.STRING)
    private FileProcessingStatus status;
    private long processed;
    private String errorMessage;
    @OneToMany(mappedBy = "batchExecution", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<BatchExecutionItem> executionItems = new ArrayList<>();
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected BatchExecution() { }

    public BatchExecution(String id, String operation, String filePath) {
        this.id = id;
        this.operation = operation;
        this.filePath = filePath;
        this.status = FileProcessingStatus.UPLOADED;
    }

    public String getId() { return id; }
    public String getOperation() { return operation; }
    public String getFilePath() { return filePath; }
    public Long getSpringBatchExecutionId() { return springBatchExecutionId; }
    public List<BatchExecutionItem> getExecutionItems() { return executionItems; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public FileProcessingStatus getStatus() { return status; }
    public long getProcessed() { return processed; }
    public String getErrorMessage() { return errorMessage; }
    public void setStatus(FileProcessingStatus status) { this.status = status; }
    public void setProcessed(long processed) { this.processed = processed; }
    public void setErrorMessage(String message) { this.errorMessage = message; }
    public void setSpringBatchExecutionId(Long id) { this.springBatchExecutionId = id; }

    @PrePersist
    void created() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void updated() { updatedAt = Instant.now(); }
}



