package com.ege.cvrag.llm;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.model.ollama.OllamaChatMessage;
import com.ege.cvrag.model.ollama.OllamaChatOptions;
import com.ege.cvrag.model.ollama.OllamaChatRequest;
import com.ege.cvrag.model.ollama.OllamaChatResponse;
import com.ege.cvrag.circuitbreaker.OllamaCircuitBreaker;
import com.ege.cvrag.model.ollama.OllamaEmbeddingRequest;
import com.ege.cvrag.model.ollama.OllamaEmbeddingResponse;
import com.ege.cvrag.retry.RetryExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Thin HTTP client for a local Ollama server. This is what Spring AI's
 * OllamaEmbeddingModel / OllamaChatModel do under the hood — here we call the
 * REST API ourselves so the mechanics are explicit:
 *   - POST /api/embeddings  -> turn text into a fixed-size vector
 *   - POST /api/chat        -> generate an answer from system + user messages
 */
@Component
public class OllamaClient {

    private final RestClient http;
    private final RetryExecutor retryExecutor;
    private final OllamaCircuitBreaker circuitBreaker;
    private final String embedModel;
    private final String chatModel;
    private final double temperature;

    public OllamaClient(@Value("${app.ollama.base-url}") String baseUrl,
                        @Value("${app.ollama.embed-model}") String embedModel,
                        @Value("${app.ollama.chat-model}") String chatModel,
                        @Value("${app.ollama.temperature}") double temperature,
                        @Value("${app.ollama.read-timeout-seconds}") int readTimeoutSeconds,
                        RetryExecutor retryExecutor,
                        OllamaCircuitBreaker circuitBreaker) {
        this.embedModel = embedModel;
        this.chatModel = chatModel;
        this.temperature = temperature;
        this.retryExecutor = retryExecutor;
        this.circuitBreaker = circuitBreaker;

        // LLM generation is slow locally, so give the read timeout plenty of room.
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(readTimeoutSeconds));

        this.http = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /** Embeds a single piece of text into a vector (768 floats for nomic-embed-text). */
    public float[] embed(String text) {
        OllamaEmbeddingResponse response = guardedCall(() -> http.post()
                .uri(RagBotConstants.OLLAMA_EMBEDDINGS_ENDPOINT)
                .body(new OllamaEmbeddingRequest(embedModel, text))
                .retrieve()
                .body(OllamaEmbeddingResponse.class));
        if (response == null || response.embedding() == null) {
            throw new IllegalStateException("Ollama returned no embedding for text: " + preview(text));
        }
        return response.embedding();
    }

    /** Runs a chat completion with a system instruction and a user message. */
    public String chat(String systemPrompt, String userPrompt) {
        OllamaChatResponse response = guardedCall(() -> http.post()
                .uri(RagBotConstants.OLLAMA_CHAT_ENDPOINT)
                .body(new OllamaChatRequest(
                        chatModel,
                        false, // stream=false -> get the whole answer in one response
                        List.of(new OllamaChatMessage(RagBotConstants.ROLE_SYSTEM, systemPrompt),
                                new OllamaChatMessage(RagBotConstants.ROLE_USER, userPrompt)),
                        new OllamaChatOptions(temperature)))
                .retrieve()
                .body(OllamaChatResponse.class));
        if (response == null || response.message() == null) {
            throw new IllegalStateException("Ollama returned no chat message");
        }
        return response.message().content();
    }

    /**
     * Wraps an Ollama HTTP call with resilience: the circuit breaker (outer) fails
     * fast during a sustained outage, and retry-with-backoff (inner) rides out a
     * brief blip. Only availability failures ({@link ResourceAccessException}) count
     * against the breaker; a fully-retried failure is one failure to the breaker.
     */
    private <T> T guardedCall(Supplier<T> httpCall) {
        circuitBreaker.acquire(); // throws CircuitOpenException if OPEN
        try {
            T result = retryExecutor.execute(httpCall, OllamaClient::isTransient);
            circuitBreaker.onSuccess();
            return result;
        } catch (ResourceAccessException ex) {
            circuitBreaker.onFailure();
            throw ex;
        }
    }

    /** Transient failures worth retrying: connection refused / read timeout. */
    private static boolean isTransient(RuntimeException ex) {
        return ex instanceof ResourceAccessException;
    }

    private static String preview(String s) {
        return s.length() <= 60 ? s : s.substring(0, 60) + "...";
    }
}
