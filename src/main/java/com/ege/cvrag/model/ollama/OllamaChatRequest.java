package com.ege.cvrag.model.ollama;

import java.util.List;

/** Request body for Ollama POST /api/chat. */
public record OllamaChatRequest(String model,
                                boolean stream,
                                List<OllamaChatMessage> messages,
                                OllamaChatOptions options) {}
