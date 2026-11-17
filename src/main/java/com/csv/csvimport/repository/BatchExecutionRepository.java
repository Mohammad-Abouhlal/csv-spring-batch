package com.csv.csvimport.repository;

import com.csv.csvimport.domain.BatchExecution;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BatchExecutionRepository extends JpaRepository<BatchExecution, String> {
	@Query("select distinct execution from BatchExecution execution left join fetch execution.executionItems where execution.id = :id")
	Optional<BatchExecution> findWithItemsById(@Param("id") String id);
}
