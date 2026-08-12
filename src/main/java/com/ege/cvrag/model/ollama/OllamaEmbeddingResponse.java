package com.ege.cvrag.model.ollama;

/** Response body from Ollama POST /api/embeddings. */
public record OllamaEmbeddingResponse(float[] embedding) {}
