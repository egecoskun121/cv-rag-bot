package com.ege.cvrag.model.github;

import java.util.Map;

/** A repo plus its language breakdown — the input `GitHubProjectFormatter` needs. */
public record GitHubProjectView(GitHubRepo repo, Map<String, Long> languages) {}
