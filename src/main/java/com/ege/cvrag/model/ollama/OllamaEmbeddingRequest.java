package com.ege.cvrag.model.ollama;

/** Request body for Ollama POST /api/embeddings. */
public record OllamaEmbeddingRequest(String model, String prompt) {}
