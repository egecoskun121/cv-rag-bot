package com.ege.cvrag.github;

import com.ege.cvrag.model.github.GitHubProjectView;
import com.ege.cvrag.model.github.GitHubRepo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GitHubProjectFormatterTest {

    private final GitHubProjectFormatter formatter = new GitHubProjectFormatter();

    @Test
    void formatsRepoAsMarkdownSectionWithTechStack() {
        GitHubRepo repo = new GitHubRepo(
                "cv-rag-bot", "Local RAG over a CV", "Java", false,
                List.of("java", "rag"), "https://github.com/x/cv-rag-bot", "2026-08-13T20:27:04Z");

        String md = formatter.format(new GitHubProjectView(repo, Map.of("Java", 9000L, "HTML", 1000L)));

        assertThat(md).startsWith("## Project: cv-rag-bot");
        assertThat(md).contains("Local RAG over a CV");
        assertThat(md).contains("Tech stack: Java 90%, HTML 10%");   // ordered by share
        assertThat(md).contains("Topics: java, rag");
        assertThat(md).contains("Repository: https://github.com/x/cv-rag-bot");
        assertThat(md).contains("Last updated: 2026-08-13");          // date only
    }

    @Test
    void techStackFallsBackToPrimaryLanguageWhenBreakdownEmpty() {
        assertThat(GitHubProjectFormatter.techStack(Map.of(), "Kotlin")).isEqualTo("Kotlin");
        assertThat(GitHubProjectFormatter.techStack(null, "Kotlin")).isEqualTo("Kotlin");
        assertThat(GitHubProjectFormatter.techStack(Map.of(), null)).isEmpty();
    }

    @Test
    void techStackOrdersByDescendingShare() {
        String stack = GitHubProjectFormatter.techStack(
                Map.of("HTML", 1000L, "Java", 3000L, "CSS", 1000L), null);
        assertThat(stack).startsWith("Java 60%");
    }
}
