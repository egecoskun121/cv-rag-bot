package com.ege.cvrag.model.ollama;

/** A single chat message (role + content) for Ollama /api/chat. */
public record OllamaChatMessage(String role, String content) {}
