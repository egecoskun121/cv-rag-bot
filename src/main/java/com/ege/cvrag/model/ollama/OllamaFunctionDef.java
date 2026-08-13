package com.ege.cvrag.model.ollama;

import java.util.Map;

/** A function/tool the model may call: name, description, and a JSON-schema parameter spec. */
public record OllamaFunctionDef(String name, String description, Map<String, Object> parameters) {}
