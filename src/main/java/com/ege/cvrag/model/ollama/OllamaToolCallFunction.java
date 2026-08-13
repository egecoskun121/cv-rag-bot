package com.ege.cvrag.model.ollama;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/** The function a tool call names, with parsed arguments. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaToolCallFunction(String name, Map<String, Object> arguments) {}
