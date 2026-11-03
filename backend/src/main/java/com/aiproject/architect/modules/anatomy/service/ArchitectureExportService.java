package com.aiproject.architect.modules.anatomy.service;

import com.aiproject.architect.modules.anatomy.dto.AnatomySceneResponse;
import com.aiproject.architect.modules.anatomy.dto.ExportRequest;
import com.aiproject.architect.modules.anatomy.dto.ExportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class ArchitectureExportService {

    private final AnatomyAnimationService animationService;

    public ExportResponse exportScene(String projectId, ExportRequest request) {
        String format = request.getExportFormat() != null ? request.getExportFormat().toUpperCase() : "HTML";
        String animFormat = request.getAnimationFormat() != null ? request.getAnimationFormat() : "MODULE_TO_MODULE";

        AnatomySceneResponse scene = switch (animFormat) {
            case "INTERNAL_EXPLODED" -> animationService.getInternalExplodedScene(projectId, request.getTargetIdentifier());
            case "EXECUTION_TRACE" -> animationService.getExecutionTraceScene(projectId, request.getTargetIdentifier());
            default -> animationService.getModuleToModuleScene(projectId);
        };

        return switch (format) {
            case "SVG" -> exportSvg(scene);
            case "PDF" -> exportPdfDoc(scene);
            case "MP4" -> exportMp4Metadata(scene);
            default -> exportStandaloneHtml(scene);
        };
    }

    private ExportResponse exportStandaloneHtml(AnatomySceneResponse scene) {
        StringBuilder assembliesSvg = new StringBuilder();
        for (AnatomySceneResponse.IsometricAssembly asm : scene.getAssemblies()) {
            assembliesSvg.append(String.format("""
                <g class="assembly" id="%s" transform="translate(%f, %f)">
                  <!-- Isometric Box Top Face -->
                  <polygon points="0,%f %f,0 %f,%f %f,%f" fill="#1e293b" stroke="#334155" stroke-width="1.5" />
                  <!-- Isometric Front Left Face -->
                  <polygon points="0,%f 0,%f %f,%f %f,%f" fill="#0f172a" stroke="#334155" stroke-width="1.5" />
                  <!-- Isometric Front Right Face -->
                  <polygon points="%f,%f %f,%f %f,%f %f,%f" fill="#172033" stroke="#334155" stroke-width="1.5" />
                  <text x="%f" y="%f" fill="#38bdf8" font-size="11" font-family="monospace" font-weight="bold">%s</text>
                </g>
                """,
                    asm.getId(), asm.getX(), asm.getY(),
                    asm.getDepth() * 0.5, asm.getWidth() * 0.866,
                    asm.getWidth() * 0.866 + asm.getDepth() * 0.5, asm.getDepth() * 0.5,
                    asm.getDepth() * 0.5, asm.getDepth() * 0.5 + 20,
                    asm.getDepth() * 0.5, asm.getDepth() * 0.5 + asm.getHeight(),
                    asm.getWidth() * 0.866, asm.getHeight(),
                    asm.getWidth() * 0.866, 0,
                    asm.getWidth() * 0.866, 0,
                    asm.getWidth() * 0.866, asm.getHeight(),
                    asm.getWidth() * 0.866 + asm.getDepth() * 0.5, asm.getDepth() * 0.5 + asm.getHeight(),
                    asm.getWidth() * 0.866 + asm.getDepth() * 0.5, asm.getDepth() * 0.5,
                    15.0, 25.0, asm.getName()
            ));
        }

        String html = String.format("""
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <title>%s — AI Project Architect Engineering Model</title>
              <style>
                body { margin: 0; background: #020617; color: #f8fafc; font-family: system-ui, sans-serif; display: flex; flex-direction: column; height: 100vh; overflow: hidden; }
                header { padding: 16px 24px; background: #0f172a; border-bottom: 1px solid #1e293b; display: flex; justify-content: space-between; align-items: center; }
                h1 { margin: 0; font-size: 16px; font-weight: 700; color: #fff; }
                .readout { font-family: monospace; font-size: 12px; color: #34d399; }
                main { flex: 1; display: flex; align-items: center; justify-content: center; position: relative; }
                svg { max-width: 90%%; max-height: 80vh; filter: drop-shadow(0 20px 30px rgba(0,0,0,0.7)); }
                footer { padding: 12px 24px; background: #090d16; border-top: 1px solid #1e293b; font-size: 12px; color: #94a3b8; display: flex; justify-content: space-between; }
              </style>
            </head>
            <body>
              <header>
                <div>
                  <h1>%s</h1>
                  <div style="font-size: 11px; color: #94a3b8; margin-top: 2px;">%s</div>
                </div>
                <div class="readout">%s</div>
              </header>
              <main>
                <svg viewBox="0 0 800 600">
                  <!-- Ground Grid -->
                  <defs>
                    <pattern id="grid" width="40" height="20" patternUnits="userSpaceOnUse">
                      <path d="M 0 10 L 20 0 L 40 10 L 20 20 Z" fill="none" stroke="#1e293b" stroke-width="0.8"/>
                    </pattern>
                  </defs>
                  <rect width="100%%" height="100%%" fill="url(#grid)" />
                  <!-- Assemblies -->
                  %s
                </svg>
              </main>
              <footer>
                <span>%s</span>
                <span style="font-family: monospace;">AI Project Architect • Anatomy Engine</span>
              </footer>
            </body>
            </html>
            """,
                scene.getTitle(), scene.getTitle(), scene.getSubtitle(), scene.getReadout(),
                assembliesSvg.toString(), scene.getHint()
        );

        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        return ExportResponse.builder()
                .exportFormat("HTML")
                .fileName("architecture-scene-" + System.currentTimeMillis() + ".html")
                .mimeType("text/html")
                .sizeBytes(bytes.length)
                .content(html)
                .build();
    }

    private ExportResponse exportSvg(AnatomySceneResponse scene) {
        String svg = String.format("""
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 900 600" width="900" height="600" style="background:#020617">
              <rect width="100%%" height="100%%" fill="#020617"/>
              <text x="30" y="40" fill="#38bdf8" font-size="16" font-family="system-ui" font-weight="bold">%s</text>
              <text x="30" y="60" fill="#94a3b8" font-size="11" font-family="monospace">%s</text>
              <!-- Engineering Base Bed -->
              <polygon points="50,200 450,50 850,200 450,350" fill="#0f172a" stroke="#334155" stroke-width="2"/>
            </svg>
            """, scene.getTitle(), scene.getReadout());

        byte[] bytes = svg.getBytes(StandardCharsets.UTF_8);
        return ExportResponse.builder()
                .exportFormat("SVG")
                .fileName("architecture-vector-" + System.currentTimeMillis() + ".svg")
                .mimeType("image/svg+xml")
                .sizeBytes(bytes.length)
                .content(svg)
                .build();
    }

    private ExportResponse exportPdfDoc(AnatomySceneResponse scene) {
        String docText = String.format("""
            AI PROJECT ARCHITECT — TECHNICAL ARCHITECTURE DOCUMENTATION
            ===========================================================
            Title: %s
            Target: %s
            Format: %s
            Live Readout: %s
            
            1. VERIFIED ASSEMBLIES:
            %s
            
            2. INTER-COMPONENT CONNECTIONS:
            %s
            
            3. EXECUTION PHASES:
            %s
            """,
                scene.getTitle(), scene.getTargetIdentifier(), scene.getAnimationFormat(), scene.getReadout(),
                scene.getAssemblies().stream().map(a -> "- " + a.getName() + " (" + a.getRole() + ")").reduce((x, y) -> x + "\n" + y).orElse("None"),
                scene.getConnections().stream().map(c -> "- " + c.getSourceAssemblyId() + " -> " + c.getTargetAssemblyId() + ": " + c.getLabel()).reduce((x, y) -> x + "\n" + y).orElse("None"),
                scene.getSteps().stream().map(s -> "Step " + s.getStepIndex() + ": " + s.getTitle() + " (" + s.getDurationMs() + "ms)").reduce((x, y) -> x + "\n" + y).orElse("None")
        );

        byte[] bytes = docText.getBytes(StandardCharsets.UTF_8);
        return ExportResponse.builder()
                .exportFormat("PDF")
                .fileName("architecture-spec-" + System.currentTimeMillis() + ".txt")
                .mimeType("application/pdf")
                .sizeBytes(bytes.length)
                .content(docText)
                .build();
    }

    private ExportResponse exportMp4Metadata(AnatomySceneResponse scene) {
        String meta = String.format("""
            {
              "pipeline": "HTML5 Canvas MediaRecorder / FFmpeg Video Capture",
              "status": "CAPTURE_PIPELINE_READY",
              "dimensions": "1920x1080",
              "fps": 60,
              "totalDurationMs": %d,
              "totalFrames": %d,
              "scene": "%s"
            }
            """,
                scene.getSteps().stream().mapToInt(AnatomySceneResponse.AnimationStep::getDurationMs).sum(),
                scene.getSteps().stream().mapToInt(AnatomySceneResponse.AnimationStep::getDurationMs).sum() * 60 / 1000,
                scene.getTitle()
        );

        return ExportResponse.builder()
                .exportFormat("MP4")
                .fileName("architecture-recording-" + System.currentTimeMillis() + ".json")
                .mimeType("application/json")
                .sizeBytes(meta.length())
                .content(meta)
                .build();
    }
}
