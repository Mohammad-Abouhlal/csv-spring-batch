package com.csv.csvimport.csv;

import com.csv.csvimport.domain.Employee;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

@Component
public class CsvFileUtils {
    public static final List<String> HEADERS = List.of("id", "personalNumber", "firstName", "lastName", "email",
            "department", "salary", "dateOfJoining", "status", "address", "phoneNumber", "jobTitle", "birthDate");
    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true).setTrim(true).get();

    public byte[] stripBom(byte[] bytes) {
        return bytes.length >= 3 && (bytes[0] & 255) == 239 && (bytes[1] & 255) == 187
                && (bytes[2] & 255) == 191 ? Arrays.copyOfRange(bytes, 3, bytes.length) : bytes;
    }

    public void write(Path path, byte[] bytes) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, stripBom(bytes), java.nio.file.StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new IllegalStateException("CSV-Datei konnte nicht gespeichert werden", e);
        }
    }

    public CSVParser open(Path path, String operation) throws IOException {
        Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        try {
            CSVParser parser = FORMAT.parse(reader);
            validateHeaders(parser.getHeaderNames(), operation);
            return parser;
        } catch (Exception e) {
            reader.close();
            throw e;
        }
    }

    public void validate(byte[] bytes, String operation) {
        try (CSVParser parser = FORMAT.parse(new java.io.StringReader(new String(stripBom(bytes), StandardCharsets.UTF_8)))) {
            validateHeaders(parser.getHeaderNames(), operation);
            if (!parser.iterator().hasNext()) throw new IllegalArgumentException("CSV-Datei ist leer");
        } catch (IOException e) {
            throw new IllegalArgumentException("CSV-Datei ist ungültig", e);
        }
    }

    private void validateHeaders(List<String> headers, String operation) {
        if (headers.size() != new LinkedHashSet<>(headers).size() || !HEADERS.containsAll(headers)
                || !headers.containsAll(List.of("id", "personalNumber"))
                || (operation.equals("import") && !headers.equals(HEADERS))
                || (operation.equals("delete") && !headers.equals(List.of("id", "personalNumber")))) {
            throw new IllegalArgumentException("Ungültige CSV-Spalten für " + operation + ": " + headers);
        }
    }

    public List<Employee> readEmployees(Path path) throws IOException {
        List<Employee> employees = new ArrayList<>();
        try (CSVParser parser = open(path, "import")) {
            for (CSVRecord record : parser) employees.add(toEmployee(record));
        }
        return employees;
    }

    public String writeEmployees(List<Employee> employees) throws IOException {
        StringWriter writer = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
            printer.printRecord(HEADERS);
            for (Employee employee : employees) {
                printer.printRecord(employee.getId(), employee.getPersonalNumber(), employee.getFirstName(),
                        employee.getLastName(), employee.getEmail(), employee.getDepartment(), employee.getSalary(),
                        employee.getDateOfJoining(), employee.getStatus(), employee.getAddress(),
                        employee.getPhoneNumber(), employee.getJobTitle(), employee.getBirthDate());
            }
        }
        return writer.toString();
    }

    public Employee toEmployee(CSVRecord record) {
        Employee employee = new Employee(id(record), personalNumber(record));
        applyFields(record, employee);
        return employee;
    }

    public Long id(CSVRecord record) {
        try { return Long.valueOf(record.get("id")); }
        catch (RuntimeException e) { throw new IllegalArgumentException("Ungültige id in Zeile " + record.getRecordNumber(), e); }
    }

    public String personalNumber(CSVRecord record) {
        String value = record.get("personalNumber");
        if (value == null || value.isBlank()) throw new IllegalArgumentException("personalNumber fehlt in Zeile " + record.getRecordNumber());
        return value;
    }

    public void applyFields(CSVRecord record, Employee employee) {
        Set<String> columns = record.toMap().keySet();
        try {
            if (columns.contains("firstName")) employee.setFirstName(text(record, "firstName"));
            if (columns.contains("lastName")) employee.setLastName(text(record, "lastName"));
            if (columns.contains("email")) employee.setEmail(text(record, "email"));
            if (columns.contains("department")) employee.setDepartment(text(record, "department"));
            if (columns.contains("salary")) employee.setSalary(number(record, "salary"));
            if (columns.contains("dateOfJoining")) employee.setDateOfJoining(date(record, "dateOfJoining"));
            if (columns.contains("status")) {
                String status = text(record, "status");
                if (status != null && !List.of("Active", "Inactive").contains(status))
                    throw new IllegalArgumentException("status muss Active oder Inactive sein");
                employee.setStatus(status);
            }
            if (columns.contains("address")) employee.setAddress(text(record, "address"));
            if (columns.contains("phoneNumber")) employee.setPhoneNumber(text(record, "phoneNumber"));
            if (columns.contains("jobTitle")) employee.setJobTitle(text(record, "jobTitle"));
            if (columns.contains("birthDate")) employee.setBirthDate(date(record, "birthDate"));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Ungültige Daten in Zeile " + record.getRecordNumber() + ": " + e.getMessage(), e);
        }
    }

    private String text(CSVRecord record, String column) {
        String value = record.get(column);
        return value.isEmpty() ? null : value;
    }

    private Double number(CSVRecord record, String column) {
        String value = text(record, column);
        return value == null ? null : Double.valueOf(value);
    }

    private LocalDate date(CSVRecord record, String column) {
        String value = text(record, column);
        return value == null ? null : LocalDate.parse(value);
    }
}


