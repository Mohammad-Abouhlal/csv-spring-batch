package com.csv.csvimport.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmployeeImportBatchJobConfig {
    public static final String JOB_NAME = "employeeImportJob";
    @Bean(JOB_NAME)
    Job employeeImportJob(EmployeeJobFactory factory) { return factory.create(JOB_NAME, "import"); }
}


