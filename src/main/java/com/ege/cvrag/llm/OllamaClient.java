package com.ege.cvrag.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

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
    private final String embedModel;
    private final String chatModel;
    private final double temperature;

    public OllamaClient(@Value("${app.ollama.base-url}") String baseUrl,
                        @Value("${app.ollama.embed-model}") String embedModel,
                        @Value("${app.ollama.chat-model}") String chatModel,
                        @Value("${app.ollama.temperature}") double temperature,
                        @Value("${app.ollama.read-timeout-seconds}") int readTimeoutSeconds) {
        this.embedModel = embedModel;
        this.chatModel = chatModel;
        this.temperature = temperature;

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
        EmbeddingResponse response = http.post()
                .uri("/api/embeddings")
                .body(new EmbeddingRequest(embedModel, text))
                .retrieve()
                .body(EmbeddingResponse.class);
        if (response == null || response.embedding() == null) {
            throw new IllegalStateException("Ollama returned no embedding for text: " + preview(text));
        }
        return response.embedding();
    }

    /** Runs a chat completion with a system instruction and a user message. */
    public String chat(String systemPrompt, String userPrompt) {
        ChatResponse response = http.post()
                .uri("/api/chat")
                .body(new ChatRequest(
                        chatModel,
                        false, // stream=false -> get the whole answer in one response
                        List.of(new Message("system", systemPrompt),
                                new Message("user", userPrompt)),
                        new Options(temperature)))
                .retrieve()
                .body(ChatResponse.class);
        if (response == null || response.message() == null) {
            throw new IllegalStateException("Ollama returned no chat message");
        }
        return response.message().content();
    }

    private static String preview(String s) {
        return s.length() <= 60 ? s : s.substring(0, 60) + "...";
    }

    // --- Ollama request/response DTOs (JSON field names match the API) ---

    private record EmbeddingRequest(String model, String prompt) {}

    private record EmbeddingResponse(float[] embedding) {}

    private record ChatRequest(String model, boolean stream, List<Message> messages, Options options) {}

    private record Options(double temperature) {}

    private record Message(String role, String content) {}

    private record ChatResponse(Message message) {}
}
