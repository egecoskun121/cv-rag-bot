package com.ege.cvrag.model.ollama;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/** Request body for Ollama POST /api/chat. {@code tools} is omitted when null. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaChatRequest(String model,
                                boolean stream,
                                List<OllamaChatMessage> messages,
                                OllamaChatOptions options,
                                List<OllamaTool> tools) {

    public OllamaChatRequest(String model, boolean stream,
                             List<OllamaChatMessage> messages, OllamaChatOptions options) {
        this(model, stream, messages, options, null);
    }
}
