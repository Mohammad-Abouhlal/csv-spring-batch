package com.csv.csvimport.batch;

import com.csv.csvimport.csv.CsvFileUtils;
import com.csv.csvimport.csv.CsvItemReader;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVRecord;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.JdbcDefaultBatchConfiguration;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.JobOperatorFactoryBean;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.Isolation;

@Configuration
@RequiredArgsConstructor
public class BatchConfig extends JdbcDefaultBatchConfiguration {
    public static final int CHUNK_SIZE = 25;


    private final ObjectProvider<Tracer> tracer;

    @Override
    protected TaskExecutor getTaskExecutor() {
        return  task -> {
            Tracer t = tracer.getIfAvailable();
            if (t == null) {
                task.run();
                return;
            }

            try (Tracer.SpanInScope ignored = t.withSpan(null)) {
                task.run();
            }
        };
    }

            @Bean(name ="batchToskExecutor")
                public TaskExecutor batchTaskExecutor() {
                    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor( "batch-");
                    ContextSnapshotFactory snapshotFactory = ContextSnapshotFactory.builder().build();

                    executor.setTaskDecorator(  runnable ->
                            snapshotFactory.captureAll().wrap(runnable)
                    );

                    return executor;
            }

//    @Bean
//    public TaskExecutor batchTaskExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(2);
//        executor.setMaxPoolSize(4);
//        executor.setQueueCapacity(100);
//        executor.setThreadNamePrefix("csv-batch-");
//        executor.initialize();
//        return executor;
//    }

    @Bean(name = "asyncJobOperator")
    public JobOperator jobOperator() throws Exception {
        JobOperatorFactoryBean factory = new JobOperatorFactoryBean();
        factory.setJobRepository(jobRepository());
        factory.setJobRegistry(getJobRegistry());
        factory.setTransactionManager(getTransactionManager());
        factory.setTaskExecutor(batchTaskExecutor());
        factory.afterPropertiesSet();
        return factory.getObject();}

    @Bean
    @StepScope
    public ItemStreamReader<CSVRecord> csvReader(CsvFileUtils files,
            @Value("#{jobParameters['file']}") String filename,
            @Value("#{jobParameters['operation']}") String operation) {
        return new CsvItemReader(files, filename, operation);
    }

    @Override
    protected Isolation getIsolationLevelForCreate() {
        return Isolation.READ_COMMITTED;
    }
}


