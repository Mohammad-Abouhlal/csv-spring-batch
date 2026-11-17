package com.csv.csvimport.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;

@Entity
public class BatchExecutionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String executionId;
    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "executionId", referencedColumnName = "id", insertable = false, updatable = false)
    private BatchExecution batchExecution;
    private Long employeeId;
    private String personalNumber;
    private String result;
    private Long rowNumber;
    @Embedded
    private BatchExecutionItemMessage message;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;

    protected BatchExecutionItem() { }

    public BatchExecutionItem(String executionId, Employee employee, String result) {
        this.executionId = executionId;
        this.employeeId = employee.getId();
        this.personalNumber = employee.getPersonalNumber();
        this.result = result;
    }

    public BatchExecutionItem(String executionId, Long employeeId, String personalNumber,
            long rowNumber, String error) {
        this.executionId = executionId;
        this.employeeId = employeeId;
        this.personalNumber = personalNumber;
        this.rowNumber = rowNumber;
        this.result = "ERROR";
        this.message = new BatchExecutionItemMessage(error);
    }

    public Long getId() { return id; }
    public String getExecutionId() { return executionId; }
    public Long getEmployeeId() { return employeeId; }
    public String getPersonalNumber() { return personalNumber; }
    public String getResult() { return result; }
    public Long getRowNumber() { return rowNumber; }
    public BatchExecutionItemMessage getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @PrePersist
    void created() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void updated() { updatedAt = Instant.now(); }
}


