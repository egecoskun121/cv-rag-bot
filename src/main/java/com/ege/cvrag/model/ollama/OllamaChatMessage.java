package com.ege.cvrag.model.ollama;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A chat message. Assistant replies may carry {@code toolCalls}; a message with
 * role {@code "tool"} carries a tool result in {@code content}. Null fields are
 * omitted so plain user/system messages serialize cleanly.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaChatMessage(String role,
                                String content,
                                @JsonProperty("tool_calls") List<OllamaToolCall> toolCalls) {

    public OllamaChatMessage(String role, String content) {
        this(role, content, null);
    }
}
