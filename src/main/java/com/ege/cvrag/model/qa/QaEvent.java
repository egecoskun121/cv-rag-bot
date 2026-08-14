package com.ege.cvrag.model.qa;

import java.util.List;

/**
 * An observability record emitted for every answered question. Carries what the
 * RAG pipeline already computed — which CV sections were retrieved, the best
 * similarity score, the answer, and how long it took — so a consumer can track
 * quality (weak-retrieval detection), popular topics and latency without touching
 * the request path.
 */
public record QaEvent(String question,
                      List<String> retrievedSections,
                      double topScore,
                      String answer,
                      long latencyMs,
                      String timestamp) {}
