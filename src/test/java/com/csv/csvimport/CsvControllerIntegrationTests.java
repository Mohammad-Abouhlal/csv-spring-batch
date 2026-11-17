package com.csv.csvimport;

import com.csv.csvimport.csv.CsvFileUtils;
import com.csv.csvimport.domain.Employee;
import com.csv.csvimport.domain.FileProcessingStatus;
import com.csv.csvimport.repository.EmployeeRepository;
import com.csv.csvimport.service.BatchExecutionItemService;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CsvControllerIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired EmployeeRepository employees;
    @Autowired BatchExecutionItemService items;

    @BeforeEach
    void clean() { employees.deleteAll(); }

    @Test
    void importCreatesEmployeeAndTracksItems() throws Exception {
        String csv = String.join(",", CsvFileUtils.HEADERS) + "\n"
                + "1,P-1,Ada,Lovelace,ada@example.com,IT,90000,2020-01-02,Active,Street,123,Engineer,1990-05-04\n";
        String id = upload("/import", csv);
        assertEquals(FileProcessingStatus.FINISHED, await(id));
        mvc.perform(get("/jobs/{id}", id)).andExpect(jsonPath("$.processed").value(1));
        assertEquals("Ada", employees.findById(1L).orElseThrow().getFirstName());
        assertEquals(1, items.items(id).size());
        mvc.perform(get("/jobs/{id}/items", id)).andExpect(status().isOk()).andExpect(jsonPath("$[0].result").value("import"));
    }

    @Test
    void updateChangesOnlyProvidedColumns() throws Exception {
        Employee employee = new Employee(2L, "P-2");
        employee.setFirstName("Before");
        employee.setDepartment("IT");
        employees.save(employee);
        String id = upload("/update", "id,personalNumber,firstName,salary\n2,P-2,After,1234.5\n");
        assertEquals(FileProcessingStatus.FINISHED, await(id));
        Employee updated = employees.findById(2L).orElseThrow();
        assertEquals("After", updated.getFirstName());
        assertEquals("IT", updated.getDepartment());
        assertEquals("P-2", updated.getPersonalNumber());
        assertEquals(1234.5, updated.getSalary());
    }

    @Test
    void deleteRequiresBothIdentifiers() throws Exception {
        employees.save(new Employee(3L, "P-3"));
        String id = upload("/delete", "id,personalNumber\n3,P-3\n");
        assertEquals(FileProcessingStatus.FINISHED, await(id));
        assertFalse(employees.existsById(3L));
        assertEquals(1, items.items(id).size());
    }

    @Test
    void invalidHeaderIsRejectedAndMismatchBecomesError() throws Exception {
        mvc.perform(multipart("/delete").file(new MockMultipartFile("file", "bad.csv", "text/csv",
                "id,wrong\n1,x\n".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest());
        employees.save(new Employee(4L, "P-4"));
        String id = upload("/delete", "id,personalNumber\n4,WRONG\n");
        assertEquals(FileProcessingStatus.ERROR, await(id));
        mvc.perform(get("/jobs/{id}", id)).andExpect(jsonPath("$.errorMessage").isNotEmpty());
        assertTrue(employees.existsById(4L));
    }

    private String upload(String path, String csv) throws Exception {
        String location = mvc.perform(multipart(path).file(new MockMultipartFile("file", "data.csv", "text/csv",
                        csv.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isAccepted()).andReturn().getResponse().getHeader("Location");
        assertNotNull(location);
        String id = location.substring(location.lastIndexOf('/') + 1);
        mvc.perform(get("/jobs/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        return id;
    }

    private FileProcessingStatus await(String id) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        FileProcessingStatus lastStatus = null;
        while (System.nanoTime() < deadline) {
            String json = mvc.perform(get("/jobs/{id}", id)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            lastStatus = FileProcessingStatus.valueOf(JsonPath.read(json, "$.status"));
            if (lastStatus == FileProcessingStatus.FINISHED || lastStatus == FileProcessingStatus.ERROR) return lastStatus;
            LockSupport.parkNanos(Duration.ofMillis(25).toNanos());
        }
        fail("Batch-Job hat das Zeitlimit überschritten: " + id + ", letzter Status: " + lastStatus);
        return null;
    }
}


