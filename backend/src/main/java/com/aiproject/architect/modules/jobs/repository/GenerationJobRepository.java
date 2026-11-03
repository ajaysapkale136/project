package com.aiproject.architect.modules.jobs.repository;

import com.aiproject.architect.modules.jobs.entity.GenerationJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GenerationJobRepository extends JpaRepository<GenerationJob, String> {
    List<GenerationJob> findAllByProjectIdOrderByStartedAtDesc(String projectId);
    Optional<GenerationJob> findTopByProjectIdOrderByStartedAtDesc(String projectId);
}
