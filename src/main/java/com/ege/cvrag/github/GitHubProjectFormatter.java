package com.ege.cvrag.github;

import com.ege.cvrag.model.github.GitHubRepo;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Formats a GitHub repository (plus its language breakdown) into a Markdown
 * section that the RAG pipeline can index. Pure — no I/O — so it is easy to test.
 */
public final class GitHubProjectFormatter {

    private GitHubProjectFormatter() {
        // utility class
    }

    public static String format(GitHubRepo repo, Map<String, Long> languages) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Project: ").append(repo.name()).append('\n');
        sb.append(repo.description()).append("\n\n");

        String stack = techStack(languages, repo.language());
        if (!stack.isBlank()) {
            sb.append("- Tech stack: ").append(stack).append('\n');
        }
        if (Objects.nonNull(repo.topics()) && !repo.topics().isEmpty()) {
            sb.append("- Topics: ").append(String.join(", ", repo.topics())).append('\n');
        }
        if (Objects.nonNull(repo.htmlUrl())) {
            sb.append("- Repository: ").append(repo.htmlUrl()).append('\n');
        }
        if (Objects.nonNull(repo.pushedAt())) {
            sb.append("- Last updated: ").append(datePart(repo.pushedAt())).append('\n');
        }
        return sb.toString().strip();
    }

    /** Language breakdown as "Java 94%, HTML 6%", ordered by share; falls back to the primary language. */
    static String techStack(Map<String, Long> languages, String fallback) {
        if (Objects.isNull(languages) || languages.isEmpty()) {
            return Objects.isNull(fallback) ? "" : fallback;
        }
        long total = languages.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return "";
        }
        return languages.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(e -> e.getKey() + " " + Math.round(100.0 * e.getValue() / total) + "%")
                .collect(Collectors.joining(", "));
    }

    /** GitHub timestamps look like 2026-08-13T20:27:04Z — keep just the date. */
    private static String datePart(String timestamp) {
        return timestamp.length() >= 10 ? timestamp.substring(0, 10) : timestamp;
    }
}
