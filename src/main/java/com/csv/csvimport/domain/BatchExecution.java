package com.csv.csvimport.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

@Entity
public class BatchExecution {
    @Id
    private String id;
    private String operation;
    private String filePath;
    @Enumerated(EnumType.STRING)
    private FileProcessingStatus status;
    private long processed;
    private String errorMessage;

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
    public FileProcessingStatus getStatus() { return status; }
    public long getProcessed() { return processed; }
    public String getErrorMessage() { return errorMessage; }
    public void setStatus(FileProcessingStatus status) { this.status = status; }
    public void setProcessed(long processed) { this.processed = processed; }
    public void setErrorMessage(String message) { this.errorMessage = message; }
}



