package com.csv.csvimport;

import com.csv.csvimport.csv.CsvFileUtils;
import com.csv.csvimport.repository.EmployeeRepository;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.awaitility.Awaitility.await;
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
class CsvAwaitilityIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired EmployeeRepository employees;

    @BeforeEach
    void clean() { employees.deleteAll(); }

    @Test
    void awaitilityWaitsForAsyncImportAndKeepsOnlyRowErrors() throws Exception {
        String csv = String.join(",", CsvFileUtils.HEADERS) + "\n"
                + "101,P-101,Ada,Lovelace,ada@example.com,IT,90000,2020-01-02,Active,Street,123,Engineer,1990-05-04\n"
                + "102,P-102,Invalid,Person,invalid@example.com,IT,wrong,2020-01-02,Active,Street,123,Engineer,1990-05-04\n"
                + "103,P-103,Grace,Hopper,grace@example.com,IT,90000,2020-01-02,Active,Street,123,Engineer,1990-05-04\n";
        String location = mvc.perform(multipart("/import").file(new MockMultipartFile(
                        "file", "data.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isAccepted()).andReturn().getResponse().getHeader("Location");
        assertNotNull(location);
        String id = location.substring(location.lastIndexOf('/') + 1);

        await().atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(25))
                .until(() -> {
                    String json = mvc.perform(get("/jobs/{id}", id)).andExpect(status().isOk())
                            .andReturn().getResponse().getContentAsString();
                    String jobStatus = JsonPath.read(json, "$.status");
                    return "FINISHED".equals(jobStatus) || "ERROR".equals(jobStatus);
                });

        mvc.perform(get("/jobs/{id}", id)).andExpect(jsonPath("$.status").value("FINISHED"))
                .andExpect(jsonPath("$.processed").value(2));
        mvc.perform(get("/jobs/{id}/items", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].result").value("ERROR"))
                .andExpect(jsonPath("$[0].rowNumber").value(2));
        assertTrue(employees.existsById(101L));
        assertFalse(employees.existsById(102L));
        assertTrue(employees.existsById(103L));
    }
}
