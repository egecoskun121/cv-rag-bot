package com.ege.cvrag.model;

/** Request body for Ollama POST /api/embeddings. */
public record OllamaEmbeddingRequest(String model, String prompt) {}
