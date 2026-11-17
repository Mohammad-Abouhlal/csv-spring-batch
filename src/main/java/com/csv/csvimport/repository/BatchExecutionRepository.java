package com.csv.csvimport.repository;

import com.csv.csvimport.domain.BatchExecution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchExecutionRepository extends JpaRepository<BatchExecution, String> { }
