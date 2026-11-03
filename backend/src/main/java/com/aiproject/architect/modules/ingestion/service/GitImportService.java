package com.aiproject.architect.modules.ingestion.service;

import com.aiproject.architect.common.exception.AppException;
import com.aiproject.architect.common.exception.SecurityValidationException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class GitImportService {

    private static final Logger log = LoggerFactory.getLogger(GitImportService.class);
    private final ZipExtractionService zipExtractionService;

    private static final Pattern GITHUB_URL_PATTERN =
            Pattern.compile("https?://github\\.com/([^/]+)/([^/\\.]+)(?:\\.git)?(?:/tree/([^/]+))?");

    public GitImportResult importRepository(String repoUrl, String branch, String token, Path destinationDir) {
        if (repoUrl == null || repoUrl.trim().isEmpty()) {
            throw new SecurityValidationException("Repository URL cannot be null or empty.");
        }

        String sanitizedUrl = repoUrl.trim();
        Matcher matcher = GITHUB_URL_PATTERN.matcher(sanitizedUrl);

        if (!matcher.find()) {
            throw new SecurityValidationException("Invalid GitHub URL format. Expected: https://github.com/owner/repository");
        }

        String owner = matcher.group(1);
        String repo = matcher.group(2);
        String targetBranch = branch != null && !branch.trim().isEmpty() ? branch.trim() :
                (matcher.group(3) != null ? matcher.group(3) : "main");

        log.info("Importing repository {}/{} on branch {}", owner, repo, targetBranch);

        // Attempt 1: Git CLI shallow clone if available
        try {
            return cloneWithGitCli(sanitizedUrl, targetBranch, token, destinationDir);
        } catch (Exception cliEx) {
            log.warn("Git CLI clone failed or unavailable ({}), falling back to GitHub Archive API", cliEx.getMessage());
            return downloadArchiveFromGitHub(owner, repo, targetBranch, token, destinationDir);
        }
    }

    private GitImportResult cloneWithGitCli(String repoUrl, String branch, String token, Path destinationDir) throws Exception {
        String authUrl = repoUrl;
        if (token != null && !token.trim().isEmpty()) {
            // Embed token safely for CLI invocation
            authUrl = repoUrl.replace("https://", "https://" + token.trim() + "@");
        }

        ProcessBuilder pb = new ProcessBuilder(
                "git", "clone", "--depth", "1", "--branch", branch, authUrl, destinationDir.toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Mask tokens from output
                if (token != null && !token.trim().isEmpty()) {
                    line = line.replace(token.trim(), "********");
                }
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished || process.exitValue() != 0) {
            throw new AppException("Git clone process exited with code " + (finished ? process.exitValue() : "TIMEOUT"));
        }

        // Get HEAD commit hash
        String commitHash = getCommitHash(destinationDir);

        return new GitImportResult(branch, commitHash);
    }

    private String getCommitHash(Path repoDir) {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
            pb.directory(repoDir.toFile());
            Process p = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String hash = reader.readLine();
                if (hash != null && !hash.trim().isEmpty()) {
                    return hash.trim();
                }
            }
        } catch (Exception ignored) {
        }
        return "UNKNOWN_COMMIT";
    }

    private GitImportResult downloadArchiveFromGitHub(String owner, String repo, String branch, String token, Path destinationDir) {
        try {
            String archiveUrl = String.format("https://api.github.com/repos/%s/%s/zipball/%s", owner, repo, branch);
            URL url = URI.create(archiveUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            conn.setRequestProperty("User-Agent", "AI-Project-Architect");

            if (token != null && !token.trim().isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + token.trim());
            }

            int code = conn.getResponseCode();
            if (code == 301 || code == 302) {
                String redirectUrl = conn.getHeaderField("Location");
                conn = (HttpURLConnection) URI.create(redirectUrl).toURL().openConnection();
                conn.setRequestProperty("User-Agent", "AI-Project-Architect");
            }

            try (InputStream in = conn.getInputStream()) {
                zipExtractionService.extractZipStream(in, destinationDir);
            }

            return new GitImportResult(branch, "SNAPSHOT-" + System.currentTimeMillis());

        } catch (Exception e) {
            log.error("Failed to download repository archive from GitHub: {}", e.getMessage(), e);
            throw new AppException("Failed to import GitHub repository: " + e.getMessage(), e);
        }
    }

    public record GitImportResult(String branch, String commitHash) {}
}
