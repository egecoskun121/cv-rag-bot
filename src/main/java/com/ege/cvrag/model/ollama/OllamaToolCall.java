package com.ege.cvrag.model.ollama;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/** A tool call the model requested. {@code id} is present on Ollama responses. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaToolCall(String id, OllamaToolCallFunction function) {}
