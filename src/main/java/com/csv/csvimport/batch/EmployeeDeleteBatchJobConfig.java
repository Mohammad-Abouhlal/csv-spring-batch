package com.csv.csvimport.batch;

import org.springframework.batch.core.job.Job;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmployeeDeleteBatchJobConfig {
    public static final String JOB_NAME = "employeeDeleteJob";
    @Bean(JOB_NAME)
    Job employeeDeleteJob(EmployeeJobFactory factory) { return factory.create(JOB_NAME, "delete"); }
}


