package com.ege.cvrag.github;

import com.ege.cvrag.model.github.GitHubRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

/**
 * Fetches the user's own (non-fork), described repositories from GitHub and
 * renders them as one Markdown section per project for indexing.
 */
@Component
public class GitHubProjectsSource {

    private final GitHubApi gitHubApi;
    private final String user;

    public GitHubProjectsSource(GitHubApi gitHubApi, @Value("${app.github.user}") String user) {
        this.gitHubApi = gitHubApi;
        this.user = user;
    }

    public String asMarkdown() {
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
