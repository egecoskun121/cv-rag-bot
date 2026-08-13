package com.ege.cvrag.ingestion;

import com.ege.cvrag.github.GitHubProjectsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Indexes the user's GitHub projects into the vector store, appended after the CV
 * ({@code @Order(2)}, so the CV runner's reload-clear has already run). A GitHub
 * failure (rate limit, network) is logged and skipped — the CV bot still works.
 */
@Component
@Order(2)
public class GitHubIngestionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GitHubIngestionRunner.class);

    private final MarkdownIndexer indexer;
    private final GitHubProjectsSource projectsSource;
    private final boolean enabled;

    public GitHubIngestionRunner(MarkdownIndexer indexer,
                                 GitHubProjectsSource projectsSource,
                                 @Value("${app.github.enabled}") boolean enabled) {
        this.indexer = indexer;
        this.projectsSource = projectsSource;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("GitHub ingestion disabled");
            return;
        }
        try {
            String markdown = projectsSource.asMarkdown();
            if (markdown.isBlank()) {
                log.info("No GitHub projects to index");
                return;
            }
            int count = indexer.index(markdown);
            log.info("Ingested {} GitHub project chunks into pgvector", count);
        } catch (RuntimeException ex) {
            log.warn("GitHub ingestion failed (continuing without projects): {}", ex.getMessage());
        }
    }
}
