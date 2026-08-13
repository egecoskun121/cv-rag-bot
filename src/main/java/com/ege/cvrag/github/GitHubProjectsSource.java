package com.ege.cvrag.github;

import com.ege.cvrag.ingestion.DocumentSource;
import com.ege.cvrag.model.github.GitHubRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

/**
 * GitHub projects as a {@link DocumentSource}: fetches the user's own (non-fork,
 * described) repositories and renders one Markdown section per project, with the
 * real language breakdown as the tech stack. Only created when
 * {@code app.github.enabled=true}, so disabling it simply removes the source.
 */
@Component
@Order(2)
@ConditionalOnProperty(prefix = "app.github", name = "enabled", havingValue = "true")
public class GitHubProjectsSource implements DocumentSource {

    private final GitHubApi gitHubApi;
    private final String user;

    public GitHubProjectsSource(GitHubApi gitHubApi, @Value("${app.github.user}") String user) {
        this.gitHubApi = gitHubApi;
        this.user = user;
    }

    @Override
    public String name() {
        return "GitHub projects (" + user + ")";
    }

    @Override
    public String markdown() {
        return gitHubApi.listRepos(user).stream()
                .filter(this::isRealProject)
                .map(repo -> GitHubProjectFormatter.format(repo, gitHubApi.languages(user, repo.name())))
                .collect(Collectors.joining("\n\n"));
    }

    /** Skip forks and repos with no description — those carry no "what it's for" signal. */
    private boolean isRealProject(GitHubRepo repo) {
        return !repo.fork() && StringUtils.hasText(repo.description());
    }
}
