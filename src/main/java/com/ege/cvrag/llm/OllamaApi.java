package com.ege.cvrag.llm;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.model.ollama.OllamaChatRequest;
import com.ege.cvrag.model.ollama.OllamaChatResponse;
import com.ege.cvrag.model.ollama.OllamaEmbeddingRequest;
import com.ege.cvrag.model.ollama.OllamaEmbeddingResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

/**
 * Declarative HTTP client for the Ollama REST API, using Spring's native
 * {@code @HttpExchange} interface clients (backed by RestClient). Spring generates
 * the implementation from this interface — no manual request building.
 */
@HttpExchange
public interface OllamaApi {

    @PostExchange(RagBotConstants.OLLAMA_EMBEDDINGS_ENDPOINT)
    OllamaEmbeddingResponse embed(@RequestBody OllamaEmbeddingRequest request);

    @PostExchange(RagBotConstants.OLLAMA_CHAT_ENDPOINT)
    OllamaChatResponse chat(@RequestBody OllamaChatRequest request);
}
