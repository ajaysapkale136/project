package com.aiproject.architect.modules.patch.repository;

import com.aiproject.architect.modules.patch.entity.IsolatedWorkspace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IsolatedWorkspaceRepository extends JpaRepository<IsolatedWorkspace, String> {
    List<IsolatedWorkspace> findByProjectIdOrderByCreatedAtDesc(String projectId);
    Optional<IsolatedWorkspace> findTopByProjectIdAndStatusOrderByCreatedAtDesc(String projectId, String status);
}
