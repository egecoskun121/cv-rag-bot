package com.ege.cvrag.github;

import com.ege.cvrag.model.github.GitHubRepo;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;
import java.util.Map;

/** Declarative client for the parts of the GitHub REST API we use. */
@HttpExchange
public interface GitHubApi {

    /** Public repositories owned by the user. */
    @GetExchange("/users/{user}/repos?per_page=100&type=owner&sort=pushed")
    List<GitHubRepo> listRepos(@PathVariable String user);

    /** Language byte-breakdown for a repository, e.g. {@code {"Java": 45000, "HTML": 3000}}. */
    @GetExchange("/repos/{owner}/{repo}/languages")
    Map<String, Long> languages(@PathVariable String owner, @PathVariable String repo);
}
