package com.csv.csvimport;

import com.csv.csvimport.csv.CsvFileUtils;
import com.csv.csvimport.repository.EmployeeRepository;
import com.csv.csvimport.service.BatchExecutionItemService;
import com.csv.csvimport.service.BatchService;
import com.jayway.jsonpath.JsonPath;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CsvSynchronousExecutorIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired EmployeeRepository employees;
    @Autowired BatchExecutionItemService items;
    @Autowired BatchService batchService;

    private Object operatorTarget;
    private TaskExecutor originalExecutor;

    @BeforeEach
    void useSynchronousExecutor() {
        employees.deleteAll();
        Object serviceTarget = AopTestUtils.getUltimateTargetObject(batchService);
        JobOperator serviceOperator = (JobOperator) ReflectionTestUtils.getField(serviceTarget, "operator");
        assertNotNull(serviceOperator, "BatchService muss einen JobOperator verwenden");
        operatorTarget = AopTestUtils.getUltimateTargetObject(serviceOperator);
        originalExecutor = (TaskExecutor) ReflectionTestUtils.getField(operatorTarget, "taskExecutor");
        assertNotNull(originalExecutor);
        ReflectionTestUtils.setField(operatorTarget, "taskExecutor", new SyncTaskExecutor());
    }

    @AfterEach
    void restoreAsynchronousExecutor() {
        if (operatorTarget != null && originalExecutor != null) {
            ReflectionTestUtils.setField(operatorTarget, "taskExecutor", originalExecutor);
        }
    }

    @Test
    void jobOperatorRunsOnTheRequestThreadWithSynchronousExecutor() throws Exception {
        Thread requestThread = Thread.currentThread();
        AtomicReference<Thread> jobThread = new AtomicReference<>();
        CountDownLatch executorInvoked = new CountDownLatch(1);
        ReflectionTestUtils.setField(operatorTarget, "taskExecutor", (TaskExecutor) task ->
                new SyncTaskExecutor().execute(() -> {
                    jobThread.set(Thread.currentThread());
                    executorInvoked.countDown();
                    task.run();
                }));

        String csv = String.join(",", CsvFileUtils.HEADERS) + "\n"
                + "205,P-205,Test,Person,test@example.com,IT,90000,2020-01-02,Active,Street,123,Engineer,1990-05-04\n";
        String location = mvc.perform(multipart("/import").file(new MockMultipartFile(
                        "file", "data.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        assertNotNull(location);
        assertTrue(executorInvoked.await(5, TimeUnit.SECONDS),
                "Der JobOperator muss den gemessenen Executor benutzen");
        assertSame(requestThread, jobThread.get(), "Der JobOperator muss im Request-Thread laufen");
        mvc.perform(get(URI.create(location))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
        assertTrue(employees.existsById(205L));
    }

    @Test
    void importIsFinishedWhenUploadReturns() throws Exception {
        String csv = String.join(",", CsvFileUtils.HEADERS) + "\n"
                + "201,P-201,Ada,Lovelace,ada@example.com,IT,90000,2020-01-02,Active,Street,123,Engineer,1990-05-04\n"
                + "202,P-202,Invalid,Person,invalid@example.com,IT,wrong,2020-01-02,Active,Street,123,Engineer,1990-05-04\n";
        String location = mvc.perform(multipart("/import").file(new MockMultipartFile(
                        "file", "data.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        assertNotNull(location);
        System.out.println("POST /import -> 201 Created, Location: " + location);

        String jobStatus = mvc.perform(get(URI.create(location))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.processed").value(1))
                .andExpect(jsonPath("$.jobName").value("employeeImportJob"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].message.exceptionName").value("IllegalArgumentException"))
                .andExpect(jsonPath("$.messages[0].message.severityLevel").value("ERROR"))
                .andReturn().getResponse().getContentAsString();
        System.out.println("GET " + location + " -> " + jobStatus);

        String itemResponse = mvc.perform(get(URI.create(location + "/items"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].result").value("ERROR"))
                .andExpect(jsonPath("$[0].rowNumber").value(2))
                .andReturn().getResponse().getContentAsString();
        System.out.println("GET " + location + "/items -> " + itemResponse);

        assertTrue(employees.existsById(201L));
        assertFalse(employees.existsById(202L));
    }

    @Test
    void successfulImportHasNoItemsAfterFinishing() throws Exception {
        String csv = String.join(",", CsvFileUtils.HEADERS) + "\n"
                + "203,P-203,Grace,Hopper,grace@example.com,IT,90000,2020-01-02,Active,Street,123,Engineer,1990-05-04\n";
        String location = mvc.perform(multipart("/import").file(new MockMultipartFile(
                        "file", "data.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        assertNotNull(location);
        System.out.println("POST /import -> 201 Created, Location: " + location);

        String jobStatus = mvc.perform(get(URI.create(location))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.processed").value(1))
                .andExpect(jsonPath("$.jobName").value("employeeImportJob"))
                .andExpect(jsonPath("$.messages").isEmpty())
                .andReturn().getResponse().getContentAsString();
        System.out.println("GET " + location + " -> " + jobStatus);
        String id = JsonPath.read(jobStatus, "$.id");
        assertTrue(items.items(id).isEmpty(), "Erfolgreiche BatchExecutionItem-Einträge müssen gelöscht sein");
        String itemResponse = mvc.perform(get(URI.create(location + "/items"))).andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty())
                .andReturn().getResponse().getContentAsString();
        System.out.println("GET " + location + "/items -> " + itemResponse);
        assertTrue(employees.existsById(203L));
    }

    @Test
    void importWithOnlyInvalidRowsEndsInErrorAndRetainsFailure() throws Exception {
        String csv = String.join(",", CsvFileUtils.HEADERS) + "\n"
                + "204,P-204,Invalid,Person,invalid@example.com,IT,wrong,2020-01-02,Active,Street,123,Engineer,1990-05-04\n";
        String location = mvc.perform(multipart("/import").file(new MockMultipartFile(
                        "file", "data.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        assertNotNull(location);
        System.out.println("POST /import -> 201 Created, Location: " + location);

        String jobStatus = mvc.perform(get(URI.create(location))).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.processed").value(0))
                .andExpect(jsonPath("$.errorMessage").isNotEmpty())
                .andExpect(jsonPath("$.messages.length()").value(1))
                .andExpect(jsonPath("$.messages[0].rowNumber").value(1))
                .andExpect(jsonPath("$.messages[0].message.exceptionName").value("IllegalArgumentException"))
                .andExpect(jsonPath("$.messages[0].message.text").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        System.out.println("GET " + location + " -> " + jobStatus);

        String itemResponse = mvc.perform(get(URI.create(location + "/items"))).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].result").value("ERROR"))
                .andExpect(jsonPath("$[0].rowNumber").value(1))
                .andReturn().getResponse().getContentAsString();
        System.out.println("GET " + location + "/items -> " + itemResponse);
        assertFalse(employees.existsById(204L));
    }
}
