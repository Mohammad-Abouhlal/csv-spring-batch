package com.csv.csvimport;

import com.csv.csvimport.csv.CsvFileUtils;
import com.csv.csvimport.repository.EmployeeRepository;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    @Autowired @Qualifier("asyncJobOperator") JobOperator jobOperator;

    private Object operatorTarget;
    private TaskExecutor originalExecutor;

    @BeforeEach
    void useSynchronousExecutor() {
        employees.deleteAll();
        operatorTarget = AopTestUtils.getUltimateTargetObject(jobOperator);
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
    void importIsFinishedWhenUploadReturns() throws Exception {
        String csv = String.join(",", CsvFileUtils.HEADERS) + "\n"
                + "201,P-201,Ada,Lovelace,ada@example.com,IT,90000,2020-01-02,Active,Street,123,Engineer,1990-05-04\n"
                + "202,P-202,Invalid,Person,invalid@example.com,IT,wrong,2020-01-02,Active,Street,123,Engineer,1990-05-04\n";
        String location = mvc.perform(multipart("/import").file(new MockMultipartFile(
                        "file", "data.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isAccepted()).andReturn().getResponse().getHeader("Location");
        assertNotNull(location);
        String id = location.substring(location.lastIndexOf('/') + 1);

        mvc.perform(get("/jobs/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.processed").value(1));
        mvc.perform(get("/jobs/{id}/items", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].result").value("ERROR"))
                .andExpect(jsonPath("$[0].rowNumber").value(2));
        assertTrue(employees.existsById(201L));
        assertFalse(employees.existsById(202L));
    }
}
