package com.aiproject.architect.modules.anatomy.repository;

import com.aiproject.architect.modules.anatomy.entity.RuntimeTraceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RuntimeTraceEventRepository extends JpaRepository<RuntimeTraceEvent, String> {
    List<RuntimeTraceEvent> findTop100ByProjectIdAndOperationOrderByOccurredAtDesc(String projectId, String operation);
}
