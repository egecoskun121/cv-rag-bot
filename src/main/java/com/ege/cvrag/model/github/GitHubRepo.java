package com.ege.cvrag.model.github;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** A subset of the GitHub repository fields we index. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GitHubRepo(String name,
                         String description,
                         String language,
                         boolean fork,
                         List<String> topics,
                         @JsonProperty("html_url") String htmlUrl,
                         @JsonProperty("pushed_at") String pushedAt) {}
