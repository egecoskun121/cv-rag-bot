package com.ege.cvrag.config;

import com.ege.cvrag.github.GitHubApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

/**
 * Builds the {@link GitHubApi} declarative client. GitHub requires a User-Agent
 * header; a token is optional (public data works unauthenticated, just with a
 * lower rate limit).
 */
@Configuration
public class GitHubClientConfig {

    @Bean
    public GitHubApi gitHubApi(@Value("${app.github.base-url}") String baseUrl,
                               @Value("${app.github.token:}") String token) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));

        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .defaultHeader("User-Agent", "cv-rag-bot")
                .defaultHeader("Accept", "application/vnd.github+json");
        if (StringUtils.hasText(token)) {
            builder.defaultHeader("Authorization", "Bearer " + token);
        }

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(builder.build()))
                .build()
                .createClient(GitHubApi.class);
    }
}
