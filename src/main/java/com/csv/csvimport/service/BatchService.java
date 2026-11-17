package com.csv.csvimport.service;

import com.csv.csvimport.batch.EmployeeDeleteBatchJobConfig;
import com.csv.csvimport.batch.EmployeeImportBatchJobConfig;
import com.csv.csvimport.batch.EmployeeUpdateBatchJobConfig;
import com.csv.csvimport.csv.CsvFileUtils;
import com.csv.csvimport.domain.FileProcessingStatus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class BatchService {
    private final Map<String, Job> jobs;
    private final JobOperator operator;
    private final BatchExecutionService executions;
    private final CsvFileUtils files;
    private final Path directory;

    public BatchService(Map<String, Job> jobs, @Qualifier("asyncJobOperator") JobOperator operator,
            BatchExecutionService executions, CsvFileUtils files, @Value("${app.csv.directory}") String directory) {
        this.jobs = jobs;
        this.operator = operator;
        this.executions = executions;
        this.files = files;
        this.directory = Path.of(directory);
    }

    public String start(String operation, byte[] bytes) {
        String jobName = switch (operation) {
            case "import" -> EmployeeImportBatchJobConfig.JOB_NAME;
            case "update" -> EmployeeUpdateBatchJobConfig.JOB_NAME;
            case "delete" -> EmployeeDeleteBatchJobConfig.JOB_NAME;
            default -> throw new IllegalArgumentException("Unbekannte Operation");
        };
        if (bytes.length == 0) throw new IllegalArgumentException("CSV-Datei ist leer");
        files.validate(bytes, operation);
        String id = UUID.randomUUID().toString();
        Path path = directory.resolve(operation).resolve(id + ".csv").toAbsolutePath();
        files.write(path, bytes);
        try {
            executions.uploaded(id, operation, jobName, path);
            executions.status(id, FileProcessingStatus.STARTED, null);
            operator.start(jobs.get(jobName), new JobParametersBuilder()
                    .addString("executionId", id)
                    .addString("file", path.toString())
                    .addString("operation", operation).toJobParameters());
            return id;
        } catch (Exception e) {
            try { Files.deleteIfExists(path); } catch (Exception ignored) { /* Originalfehler bleibt erhalten. */ }
            executions.status(id, FileProcessingStatus.ERROR, e.getMessage());
            throw new IllegalStateException("Batch-Job konnte nicht gestartet werden", e);
        }
    }
}


