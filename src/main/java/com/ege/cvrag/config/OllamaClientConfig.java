package com.ege.cvrag.config;

import com.ege.cvrag.llm.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.time.Duration;

/**
 * Builds the {@link OllamaApi} declarative client over a RestClient, wiring the
 * base URL and timeouts (LLM generation is slow locally, so the read timeout is
 * generous).
 */
@Configuration
public class OllamaClientConfig {

    @Bean
    public OllamaApi ollamaApi(@Value("${app.ollama.base-url}") String baseUrl,
                               @Value("${app.ollama.read-timeout-seconds}") int readTimeoutSeconds) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        RestClient restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(OllamaApi.class);
    }
}
