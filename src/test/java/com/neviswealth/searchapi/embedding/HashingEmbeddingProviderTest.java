package com.neviswealth.searchapi.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.neviswealth.searchapi.config.AppEmbeddingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HashingEmbeddingProviderTest {

    private HashingEmbeddingProvider provider;

    @BeforeEach
    void setUp() {
        AppEmbeddingProperties properties = new AppEmbeddingProperties();
        properties.getHashing().setDimensions(384);
        provider = new HashingEmbeddingProvider(properties);
    }

    @Test
    void sameInputSameVector() {
        float[] first = provider.embed("address proof");
        float[] second = provider.embed("address proof");

        assertThat(first).containsExactly(second);
    }

    @Test
    void dimensionLength() {
        float[] embedding = provider.embed("some text");

        assertThat(embedding).hasSize(384);
    }

    @Test
    void l2Normalized() {
        float[] embedding = provider.embed("hello world");

        float sumSquares = 0.0f;
        for (float value : embedding) {
            sumSquares += value * value;
        }

        assertThat(sumSquares).isCloseTo(1.0f, within(0.00001f));
    }

    @Test
    void emptyInputReturnsZeroVector() {
        float[] embedding = provider.embed("");

        assertThat(embedding).hasSize(384);
        assertThat(embedding).containsOnly(0.0f);
    }

    @Test
    void nullInputReturnsZeroVector() {
        float[] embedding = provider.embed(null);

        assertThat(embedding).hasSize(384);
        assertThat(embedding).containsOnly(0.0f);
    }

    @Test
    void nonAsciiDoesNotThrow() {
        float[] embedding = provider.embed("café 日本語");

        assertThat(embedding).hasSize(384);
    }

    @Test
    void differentInputsDifferentVectors() {
        float[] hello = provider.embed("hello");
        float[] goodbye = provider.embed("goodbye");

        assertThat(hello).isNotEqualTo(goodbye);
    }

    @Test
    void shortTokensAreFiltered() {
        float[] embedding = provider.embed("a bb");

        assertThat(embedding).hasSize(384);

        float sumSquares = 0.0f;
        for (float value : embedding) {
            sumSquares += value * value;
        }
        assertThat(sumSquares).isCloseTo(1.0f, within(0.00001f));
    }
}
