package com.aiproject.architect.modules.testing.repository;

import com.aiproject.architect.modules.testing.entity.TestExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestExecutionRepository extends JpaRepository<TestExecution, String> {
    List<TestExecution> findByProjectIdOrderByCreatedAtDesc(String projectId);
    List<TestExecution> findByChangeSetIdOrderByCreatedAtDesc(String changeSetId);
}
