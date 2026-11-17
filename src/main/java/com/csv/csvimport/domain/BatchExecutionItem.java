package com.csv.csvimport.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class BatchExecutionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String executionId;
    private Long employeeId;
    private String personalNumber;
    private String result;

    protected BatchExecutionItem() { }

    public BatchExecutionItem(String executionId, Employee employee, String result) {
        this.executionId = executionId;
        this.employeeId = employee.getId();
        this.personalNumber = employee.getPersonalNumber();
        this.result = result;
    }

    public Long getId() { return id; }
    public String getExecutionId() { return executionId; }
    public Long getEmployeeId() { return employeeId; }
    public String getPersonalNumber() { return personalNumber; }
    public String getResult() { return result; }
}


