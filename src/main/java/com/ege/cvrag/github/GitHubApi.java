package com.ege.cvrag.github;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.model.github.GitHubRepo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;
import java.util.Map;

/**
 * Declarative client for the parts of the GitHub REST API we use.
 * {@code @Cacheable} is a no-op unless {@code app.cache.enabled=true}
 * ({@link com.ege.cvrag.cache.CacheConfig} is the only place that registers the
 * caching aspect) — when enabled, it also cuts down on GitHub's rate limit.
 */
@HttpExchange
public interface GitHubApi {

    /**
     * Public repositories owned by the user. {@code unless} skips caching an empty
     * result — GitHub can return zero repos on a transient hiccup without erroring
     * (no exception to trip the "never cache a failure" default), and we don't
     * want that to lock in "no projects" for a full TTL.
     */
    @Cacheable(value = RagBotConstants.CACHE_GITHUB_REPOS, key = "#user",
            unless = "#result == null || #result.isEmpty()")
    @GetExchange("/users/{user}/repos?per_page=100&type=owner&sort=pushed")
    List<GitHubRepo> listRepos(@PathVariable String user);

    /** Language byte-breakdown for a repository, e.g. {@code {"Java": 45000, "HTML": 3000}}. */
    @Cacheable(value = RagBotConstants.CACHE_GITHUB_LANGUAGES, key = "#owner + ':' + #repo",
            unless = "#result == null || #result.isEmpty()")
    @GetExchange("/repos/{owner}/{repo}/languages")
    Map<String, Long> languages(@PathVariable String owner, @PathVariable String repo);
}
