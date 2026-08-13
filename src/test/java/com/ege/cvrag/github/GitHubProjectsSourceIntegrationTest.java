package com.ege.cvrag.github;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: real {@link GitHubProjectsSource} + {@link GitHubApi} against
 * an in-process GitHub stub. Verifies filtering (forks and description-less repos
 * are skipped) and that the language breakdown becomes a tech-stack line.
 */
class GitHubProjectsSourceIntegrationTest {

    private static final String REPOS_JSON = """
            [
              {"name":"realrepo","description":"A real project","language":"Java","fork":false,
               "topics":["java"],"html_url":"https://github.com/testuser/realrepo","pushed_at":"2026-01-01T00:00:00Z"},
              {"name":"forked","description":"a fork","language":"Java","fork":true,
               "topics":[],"html_url":"https://github.com/testuser/forked","pushed_at":"2025-01-01T00:00:00Z"},
              {"name":"nodesc","description":null,"language":"Java","fork":false,
               "topics":[],"html_url":"https://github.com/testuser/nodesc","pushed_at":"2025-01-01T00:00:00Z"}
            ]""";
    private static final String LANGUAGES_JSON = "{\"Java\":800,\"HTML\":200}";

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void indexesOnlyRealProjectsWithTechStack() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/users/testuser/repos", exchange -> respond(exchange, REPOS_JSON));
        server.createContext("/repos/", exchange -> respond(exchange, LANGUAGES_JSON));
        server.start();

        GitHubProjectsSource source = new GitHubProjectsSource(
                gitHubApiFor("http://localhost:" + server.getAddress().getPort()), "testuser");

        String markdown = source.asMarkdown();

        assertThat(markdown).contains("## Project: realrepo");
        assertThat(markdown).contains("A real project");
        assertThat(markdown).contains("Tech stack: Java 80%, HTML 20%");
        assertThat(markdown).doesNotContain("## Project: forked");   // fork filtered
        assertThat(markdown).doesNotContain("## Project: nodesc");   // no description filtered
    }

    private GitHubApi gitHubApiFor(String baseUrl) {
        RestClient client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "test")
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(GitHubApi.class);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
