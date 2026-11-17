package com.csv.csvimport.batch;

import com.csv.csvimport.csv.CsvFileUtils;
import com.csv.csvimport.domain.Employee;
import com.csv.csvimport.domain.FileProcessingStatus;
import com.csv.csvimport.repository.EmployeeRepository;
import com.csv.csvimport.service.BatchExecutionItemService;
import com.csv.csvimport.service.BatchExecutionService;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.commons.csv.CSVRecord;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class EmployeeJobFactory {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactions;
    private final ItemStreamReader<CSVRecord> reader;
    private final EmployeeRepository employees;
    private final BatchExecutionService executions;
    private final BatchExecutionItemService items;
    private final CsvFileUtils csv;

    public EmployeeJobFactory(JobRepository jobRepository, PlatformTransactionManager transactions,
            ItemStreamReader<CSVRecord> reader, EmployeeRepository employees, BatchExecutionService executions,
            BatchExecutionItemService items, CsvFileUtils csv) {
        this.jobRepository = jobRepository;
        this.transactions = transactions;
        this.reader = reader;
        this.employees = employees;
        this.executions = executions;
        this.items = items;
        this.csv = csv;
    }

    public Job create(String name, String operation) {
        Step step = new StepBuilder(name + "Step", jobRepository)
                .<CSVRecord, Employee>chunk(BatchConfig.CHUNK_SIZE, transactions)
                .reader(reader)
                .processor(record -> {
                    if (operation.equals("import")) return csv.toEmployee(record);
                    return employees.findByIdAndPersonalNumber(csv.id(record), csv.personalNumber(record))
                            .map(employee -> {
                                if (operation.equals("update")) csv.applyFields(record, employee);
                                return employee;
                            }).orElseThrow(() -> new IllegalArgumentException("Mitarbeiter in Zeile "
                                    + record.getRecordNumber() + " nicht gefunden"));
                })
                .writer(chunk -> {
                    String id = org.springframework.batch.core.scope.context.StepSynchronizationManager.getContext()
                            .getStepExecution().getJobParameters().getString("executionId");
                    for (Employee employee : chunk) {
                        if (operation.equals("delete")) employees.delete(employee);
                        else employees.save(employee);
                        items.record(id, employee, operation);
                    }
                }).build();
        return new JobBuilder(name, jobRepository).start(step).listener(new JobExecutionListener() {
            @Override
            public void beforeJob(org.springframework.batch.core.job.JobExecution execution) {
                executions.status(execution.getJobParameters().getString("executionId"), FileProcessingStatus.IN_PROGRESS, null);
            }

            @Override
            public void afterJob(org.springframework.batch.core.job.JobExecution execution) {
                String id = execution.getJobParameters().getString("executionId");
                long count = execution.getStepExecutions().stream().mapToLong(s -> s.getWriteCount()).sum();
                executions.progress(id, count);
                boolean success = execution.getStatus() == org.springframework.batch.core.BatchStatus.COMPLETED;
                String error = execution.getAllFailureExceptions().isEmpty() ? "Batch-Job fehlgeschlagen"
                        : execution.getAllFailureExceptions().get(0).getMessage();
                executions.status(id, success ? FileProcessingStatus.FINISHED : FileProcessingStatus.ERROR,
                        success ? null : error);
                try { Files.deleteIfExists(Path.of(execution.getJobParameters().getString("file"))); }
                catch (Exception ignored) { /* Jobstatus bleibt auch bei einem Aufräumfehler abrufbar. */ }
            }
        }).build();
    }
}


