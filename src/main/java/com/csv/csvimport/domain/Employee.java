package com.csv.csvimport.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.LocalDate;

@Entity
public class Employee {
    @Id
    private Long id;
    @Column(nullable = false, unique = true)
    private String personalNumber;
    private String firstName;
    private String lastName;
    private String email;
    private String department;
    private Double salary;
    private LocalDate dateOfJoining;
    private String status;
    private String address;
    private String phoneNumber;
    private String jobTitle;
    private LocalDate birthDate;

    protected Employee() { }

    public Employee(Long id, String personalNumber) {
        this.id = id;
        this.personalNumber = personalNumber;
    }

    public Long getId() { return id; }
    public String getPersonalNumber() { return personalNumber; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getDepartment() { return department; }
    public Double getSalary() { return salary; }
    public LocalDate getDateOfJoining() { return dateOfJoining; }
    public String getStatus() { return status; }
    public String getAddress() { return address; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getJobTitle() { return jobTitle; }
    public LocalDate getBirthDate() { return birthDate; }

    public void setFirstName(String value) { firstName = value; }
    public void setLastName(String value) { lastName = value; }
    public void setEmail(String value) { email = value; }
    public void setDepartment(String value) { department = value; }
    public void setSalary(Double value) { salary = value; }
    public void setDateOfJoining(LocalDate value) { dateOfJoining = value; }
    public void setStatus(String value) { status = value; }
    public void setAddress(String value) { address = value; }
    public void setPhoneNumber(String value) { phoneNumber = value; }
    public void setJobTitle(String value) { jobTitle = value; }
    public void setBirthDate(LocalDate value) { birthDate = value; }
}


