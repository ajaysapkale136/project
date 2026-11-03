package com.aiproject.architect.modules.assistant.service;

import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class DiffEngine {

    @Data
    @Builder
    public static class DiffResult {
        private String unifiedDiff;
        private int additions;
        private int deletions;
    }

    public DiffResult computeDiff(String filePath, String original, String proposed) {
        if (original == null) original = "";
        if (proposed == null) proposed = "";

        List<String> origLines = original.isEmpty() ? Collections.emptyList() : Arrays.asList(original.split("\\r?\\n"));
        List<String> propLines = proposed.isEmpty() ? Collections.emptyList() : Arrays.asList(proposed.split("\\r?\\n"));

        StringBuilder diff = new StringBuilder();
        diff.append("--- a/").append(filePath).append("\n");
        diff.append("+++ b/").append(filePath).append("\n");

        if (origLines.isEmpty() && propLines.isEmpty()) {
            return DiffResult.builder().unifiedDiff(diff.toString()).additions(0).deletions(0).build();
        }

        if (origLines.isEmpty()) {
            diff.append("@@ -0,0 +1,").append(propLines.size()).append(" @@\n");
            for (String line : propLines) {
                diff.append("+").append(line).append("\n");
            }
            return DiffResult.builder()
                    .unifiedDiff(diff.toString())
                    .additions(propLines.size())
                    .deletions(0)
                    .build();
        }

        if (propLines.isEmpty()) {
            diff.append("@@ -1,").append(origLines.size()).append(" +0,0 @@\n");
            for (String line : origLines) {
                diff.append("-").append(line).append("\n");
            }
            return DiffResult.builder()
                    .unifiedDiff(diff.toString())
                    .additions(0)
                    .deletions(origLines.size())
                    .build();
        }

        // Compute LCS (Longest Common Subsequence) for clean unified diff
        int n = origLines.size();
        int m = propLines.size();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (origLines.get(i).equals(propLines.get(j))) {
                    dp[i + 1][j + 1] = dp[i][j] + 1;
                } else {
                    dp[i + 1][j + 1] = Math.max(dp[i + 1][j], dp[i][j + 1]);
                }
            }
        }

        List<String> diffLines = new ArrayList<>();
        int i = n, j = m;
        int additions = 0;
        int deletions = 0;

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && origLines.get(i - 1).equals(propLines.get(j - 1))) {
                diffLines.add(" " + origLines.get(i - 1));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                diffLines.add("+" + propLines.get(j - 1));
                additions++;
                j--;
            } else if (i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j])) {
                diffLines.add("-" + origLines.get(i - 1));
                deletions++;
                i--;
            }
        }

        Collections.reverse(diffLines);
        diff.append("@@ -1,").append(n).append(" +1,").append(m).append(" @@\n");
        for (String dl : diffLines) {
            diff.append(dl).append("\n");
        }

        return DiffResult.builder()
                .unifiedDiff(diff.toString())
                .additions(additions)
                .deletions(deletions)
                .build();
    }
}
