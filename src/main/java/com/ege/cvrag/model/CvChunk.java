package com.ege.cvrag.model;

/**
 * A retrieved CV chunk plus its similarity score to the query.
 * {@code score} is cosine similarity in [0,1] — higher means more relevant.
 */
public record CvChunk(String content, String section, double score) {}
