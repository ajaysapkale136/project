package com.aiproject.architect.modules.patch.repository;

import com.aiproject.architect.modules.patch.entity.ChangeSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChangeSetRepository extends JpaRepository<ChangeSet, String> {
    List<ChangeSet> findByProjectIdOrderByCreatedAtDesc(String projectId);
    List<ChangeSet> findByWorkspaceIdOrderByCreatedAtDesc(String workspaceId);
}
