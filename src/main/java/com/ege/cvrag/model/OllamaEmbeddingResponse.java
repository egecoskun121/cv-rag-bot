package com.ege.cvrag.model;

/** Response body from Ollama POST /api/embeddings. */
public record OllamaEmbeddingResponse(float[] embedding) {}
