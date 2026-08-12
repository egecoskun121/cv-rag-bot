package com.ege.cvrag.model;

/** Response body from Ollama POST /api/chat (we only need the message). */
public record OllamaChatResponse(OllamaChatMessage message) {}
