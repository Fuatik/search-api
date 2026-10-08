package com.neviswealth.searchapi.embedding;

/**
 * Provides deterministic text embeddings with a fixed dimension of 384.
 * Null or blank input must return a zero vector.
 */
public interface EmbeddingProvider {

    float[] embed(String text);
}
