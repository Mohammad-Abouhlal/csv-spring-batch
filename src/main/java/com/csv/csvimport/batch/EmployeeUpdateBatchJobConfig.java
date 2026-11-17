package com.csv.csvimport.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmployeeUpdateBatchJobConfig {
    public static final String JOB_NAME = "employeeUpdateJob";
    @Bean(JOB_NAME)
    Job employeeUpdateJob(EmployeeJobFactory factory) { return factory.create(JOB_NAME, "update"); }
}


