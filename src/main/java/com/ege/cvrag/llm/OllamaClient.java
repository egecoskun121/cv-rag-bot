package com.ege.cvrag.llm;

import com.ege.cvrag.circuitbreaker.OllamaCircuitBreaker;
import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.model.ollama.OllamaChatMessage;
import com.ege.cvrag.model.ollama.OllamaChatOptions;
import com.ege.cvrag.model.ollama.OllamaChatRequest;
import com.ege.cvrag.model.ollama.OllamaChatResponse;
import com.ege.cvrag.model.ollama.OllamaEmbeddingRequest;
import com.ege.cvrag.model.ollama.OllamaEmbeddingResponse;
import com.ege.cvrag.model.ollama.OllamaTool;
import com.ege.cvrag.retry.RetryExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Service around the {@link OllamaApi} declarative client that adds resilience
 * (circuit breaker + retry) and response validation. The raw HTTP transport lives
 * in {@code OllamaApi}; this class owns the "how do we call it safely" concern.
 */
@Component
public class OllamaClient {

    private final OllamaApi api;
    private final RetryExecutor retryExecutor;
    private final OllamaCircuitBreaker circuitBreaker;
    private final String embedModel;
    private final String chatModel;
    private final double temperature;

    public OllamaClient(@Value("${app.ollama.embed-model}") String embedModel,
                        @Value("${app.ollama.chat-model}") String chatModel,
                        @Value("${app.ollama.temperature}") double temperature,
                        RetryExecutor retryExecutor,
                        OllamaCircuitBreaker circuitBreaker,
                        OllamaApi api) {
        this.embedModel = embedModel;
        this.chatModel = chatModel;
        this.temperature = temperature;
        this.retryExecutor = retryExecutor;
        this.circuitBreaker = circuitBreaker;
        this.api = api;
    }

    /** Embeds a single piece of text into a vector (1024 floats for bge-m3). */
    public float[] embed(String text) {
        OllamaEmbeddingResponse response =
                guardedCall(() -> api.embed(new OllamaEmbeddingRequest(embedModel, text)));
        if (Objects.isNull(response) || Objects.isNull(response.embedding())) {
            throw new IllegalStateException("Ollama returned no embedding for text: " + preview(text));
        }
        return response.embedding();
    }

    /** Runs a chat completion with a system instruction and a user message. */
    public String chat(String systemPrompt, String userPrompt) {
        OllamaChatResponse response = guardedCall(() -> api.chat(new OllamaChatRequest(
                chatModel,
                false, // stream=false -> get the whole answer in one response
                List.of(new OllamaChatMessage(RagBotConstants.ROLE_SYSTEM, systemPrompt),
                        new OllamaChatMessage(RagBotConstants.ROLE_USER, userPrompt)),
                new OllamaChatOptions(temperature))));
        if (Objects.isNull(response) || Objects.isNull(response.message())) {
            throw new IllegalStateException("Ollama returned no chat message");
        }
        return response.message().content();
    }

    /**
     * Chat with tools available. Returns the raw assistant message, which may
     * carry {@code toolCalls} instead of content — the caller (agent loop) runs
     * the tools and calls back with the results.
     */
    public OllamaChatMessage chatWithTools(List<OllamaChatMessage> messages, List<OllamaTool> tools) {
        OllamaChatResponse response = guardedCall(() -> api.chat(new OllamaChatRequest(
                chatModel, false, messages, new OllamaChatOptions(temperature), tools)));
        if (Objects.isNull(response) || Objects.isNull(response.message())) {
            throw new IllegalStateException("Ollama returned no chat message");
        }
        return response.message();
    }

    /**
     * Wraps an Ollama call with resilience: the circuit breaker (outer) fails fast
     * during a sustained outage, and retry-with-backoff (inner) rides out a brief
     * blip. Only availability failures ({@link ResourceAccessException}) count
     * against the breaker; a fully-retried failure is one failure to the breaker.
     */
    private <T> T guardedCall(Supplier<T> call) {
        circuitBreaker.acquire(); // throws CircuitOpenException if OPEN
        try {
            T result = retryExecutor.execute(call, OllamaClient::isTransient);
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
