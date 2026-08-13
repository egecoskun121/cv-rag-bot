package com.ege.cvrag.agent;

import com.ege.cvrag.circuitbreaker.OllamaCircuitBreaker;
import com.ege.cvrag.llm.OllamaApi;
import com.ege.cvrag.llm.OllamaClient;
import com.ege.cvrag.model.ollama.OllamaFunctionDef;
import com.ege.cvrag.model.ollama.OllamaTool;
import com.ege.cvrag.retry.RetryExecutor;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the agent harness (tool-use loop). It scripts the Ollama
 * /api/chat responses with an in-process HTTP stub: the first reply is a tool
 * call, the second is the final answer. A fake tool records that it ran. No real
 * Ollama, model, or pgvector — the loop logic is exercised deterministically.
 */
class AgentServiceIntegrationTest {

    private static final String TOOL_CALL_REPLY =
            "{\"message\":{\"role\":\"assistant\",\"content\":\"\",\"tool_calls\":"
                    + "[{\"function\":{\"name\":\"search_cv\",\"arguments\":{\"query\":\"ilaBank\"}}}]}}";
    private static final String FINAL_REPLY =
            "{\"message\":{\"role\":\"assistant\",\"content\":\"Final answer from the agent.\"}}";

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void runsToolThenReturnsFinalAnswer() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/chat", exchange -> {
            String body = calls.incrementAndGet() == 1 ? TOOL_CALL_REPLY : FINAL_REPLY;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        RecordingTool tool = new RecordingTool();
        AgentService agent = new AgentService(
                ollamaClientFor("http://localhost:" + server.getAddress().getPort()),
                List.of(tool),
                5);

        String answer = agent.ask("What did Ege do at ilaBank?");

        assertThat(answer).isEqualTo("Final answer from the agent.");
        assertThat(tool.invoked).isTrue();
        assertThat(tool.lastQuery).isEqualTo("ilaBank");
        assertThat(calls.get()).isEqualTo(2); // one tool round + one final answer
    }

    private OllamaClient ollamaClientFor(String baseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        RestClient restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        OllamaApi api = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(OllamaApi.class);
        return new OllamaClient("embed-model", "chat-model", 0.0,
                new RetryExecutor(3, 1, 2), new OllamaCircuitBreaker(3, 100), api);
    }

    /** A fake tool that records its invocation. */
    private static final class RecordingTool implements AgentTool {
        boolean invoked;
        String lastQuery;

        @Override
        public String name() {
            return "search_cv";
        }

        @Override
        public OllamaTool definition() {
            return OllamaTool.function(new OllamaFunctionDef("search_cv", "test", Map.of()));
        }

        @Override
        public String execute(Map<String, Object> arguments) {
            invoked = true;
            lastQuery = String.valueOf(arguments.get("query"));
            return "TOOL RESULT";
        }
    }
}
