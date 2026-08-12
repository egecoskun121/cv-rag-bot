package com.ege.cvrag.llm;

import com.ege.cvrag.circuitbreaker.CircuitOpenException;
import com.ege.cvrag.circuitbreaker.OllamaCircuitBreaker;
import com.ege.cvrag.retry.RetryExecutor;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for the resilience stack: real OllamaClient + OllamaApi
 * (@HttpExchange) + RetryExecutor + OllamaCircuitBreaker wired together, exercised
 * against an in-process HTTP stub (healthy path) and a dead port (outage). No
 * Spring context, no real Ollama.
 */
class OllamaClientResilienceIntegrationTest {

    private static final int FAILURE_THRESHOLD = 3;

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private OllamaClient clientFor(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        RestClient restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        OllamaApi api = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(OllamaApi.class);

        RetryExecutor retry = new RetryExecutor(3, 1, 2); // fast retries for tests
        OllamaCircuitBreaker breaker = new OllamaCircuitBreaker(FAILURE_THRESHOLD, 100);
        return new OllamaClient("embed-model", "chat-model", 0.0, retry, breaker, api);
    }

    @Test
    void embedReturnsVectorWhenServerIsHealthy() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/embeddings", exchange -> {
            byte[] body = "{\"embedding\":[0.1,0.2,0.3]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        OllamaClient client = clientFor("http://localhost:" + server.getAddress().getPort());

        float[] embedding = client.embed("hello");

        assertThat(embedding).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    void circuitOpensAfterRepeatedFailures() {
        OllamaClient client = clientFor("http://localhost:" + deadPort());

        // Each call retries then fails with a connection error…
        for (int i = 0; i < FAILURE_THRESHOLD; i++) {
            assertThatThrownBy(() -> client.embed("x"))
                    .isInstanceOf(ResourceAccessException.class);
        }

        // …after the threshold, the breaker is OPEN and fails fast without calling out.
        assertThatThrownBy(() -> client.embed("x"))
                .isInstanceOf(CircuitOpenException.class);
    }

    /** Grabs a port and immediately frees it, so connections to it are refused. */
    private int deadPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            throw new IllegalStateException("Could not allocate a dead port", e);
        }
    }
}
