package com.aiproject.architect.modules.patch.repository;

import com.aiproject.architect.modules.patch.entity.ChangeSetFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChangeSetFileRepository extends JpaRepository<ChangeSetFile, String> {
    List<ChangeSetFile> findByChangeSetId(String changeSetId);
}
