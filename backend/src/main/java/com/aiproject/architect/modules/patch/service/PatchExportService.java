package com.aiproject.architect.modules.patch.service;

import com.aiproject.architect.common.exception.ResourceNotFoundException;
import com.aiproject.architect.modules.patch.entity.ChangeSet;
import com.aiproject.architect.modules.patch.entity.ChangeSetFile;
import com.aiproject.architect.modules.patch.entity.IsolatedWorkspace;
import com.aiproject.architect.modules.patch.repository.ChangeSetFileRepository;
import com.aiproject.architect.modules.patch.repository.ChangeSetRepository;
import com.aiproject.architect.modules.patch.repository.IsolatedWorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class PatchExportService {

    private final ChangeSetRepository changeSetRepository;
    private final ChangeSetFileRepository fileChangeRepository;
    private final IsolatedWorkspaceRepository workspaceRepository;

    public String exportUnifiedPatch(String changeSetId) {
        ChangeSet cs = changeSetRepository.findById(changeSetId)
                .orElseThrow(() -> new ResourceNotFoundException("Change set not found: " + changeSetId));

        List<ChangeSetFile> files = fileChangeRepository.findByChangeSetId(changeSetId);
        StringBuilder patch = new StringBuilder();
        patch.append("# AI Project Architect Patch: ").append(cs.getTitle()).append("\n");
        patch.append("# Target Type: ").append(cs.getTargetType()).append(" | Target: ").append(cs.getTargetIdentifier()).append("\n");
        patch.append("# Status: ").append(cs.getStatus()).append(" | Risk: ").append(cs.getRiskLevel()).append("\n\n");

        for (ChangeSetFile file : files) {
            patch.append(file.getDiffUnified()).append("\n\n");
        }

        return patch.toString();
    }

    public byte[] exportWorkspaceZip(String workspaceId) throws IOException {
        IsolatedWorkspace ws = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace not found: " + workspaceId));

        Path rootPath = Paths.get(ws.getWorkspacePath());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            if (Files.exists(rootPath)) {
                try (var stream = Files.walk(rootPath)) {
                    stream.filter(Files::isRegularFile).forEach(path -> {
                        String relativePath = rootPath.relativize(path).toString().replace('\\', '/');
                        try {
                            zos.putNextEntry(new ZipEntry(relativePath));
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            // skip unreadable
                        }
                    });
                }
            }
        }

        return baos.toByteArray();
    }
}
