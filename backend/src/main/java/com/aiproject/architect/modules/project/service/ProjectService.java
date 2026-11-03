package com.aiproject.architect.modules.project.service;

import com.aiproject.architect.common.exception.ResourceNotFoundException;
import com.aiproject.architect.modules.ingestion.repository.SourceSnapshotRepository;
import com.aiproject.architect.modules.project.dto.ProjectCreateRequest;
import com.aiproject.architect.modules.project.dto.ProjectResponse;
import com.aiproject.architect.modules.project.entity.Project;
import com.aiproject.architect.modules.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final SourceSnapshotRepository snapshotRepository;

    @Transactional
    public ProjectResponse createProject(ProjectCreateRequest request) {
        Project project = Project.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .repositoryUrl(request.getRepositoryUrl())
                .defaultBranch(request.getDefaultBranch() != null ? request.getDefaultBranch() : "main")
                .status("CREATED")
                .build();
        Project saved = projectRepository.save(project);
        return mapToResponse(saved);
    }

    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ProjectResponse getProjectById(String id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
        return mapToResponse(project);
    }

    public Project getEntityById(String id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    @Transactional
    public void updateStatus(String id, String status) {
        Project project = getEntityById(id);
        project.setStatus(status);
        projectRepository.save(project);
    }

    private ProjectResponse mapToResponse(Project project) {
        ProjectResponse response = ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .repositoryUrl(project.getRepositoryUrl())
                .defaultBranch(project.getDefaultBranch())
                .status(project.getStatus())
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();

        snapshotRepository.findTopByProjectIdOrderByCreatedAtDesc(project.getId()).ifPresent(snapshot -> {
            response.setFileCount(snapshot.getFileCount());
            response.setTotalBytes(snapshot.getTotalBytes());
        });

        return response;
    }
}
