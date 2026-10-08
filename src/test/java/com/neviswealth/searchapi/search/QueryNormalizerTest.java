package com.neviswealth.searchapi.search;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class QueryNormalizerTest {

    @Test
    void nullInputReturnsEmpty() {
        assertThat(QueryNormalizer.normalize(null)).isEmpty();
    }

    @Test
    void blankInputReturnsEmpty() {
        assertThat(QueryNormalizer.normalize("")).isEmpty();
    }

    @Test
    void normalizesWhitespaceCaseAndPunctuation() {
        assertThat(QueryNormalizer.normalize("  Address   Proof! ")).isEqualTo("address proof");
    }

    @Test
    void hyphenBecomesSpace() {
        assertThat(QueryNormalizer.normalize("UTILITY-BILL")).isEqualTo("utility bill");
    }

    @Test
    void preservesNonAsciiLetters() {
        assertThat(QueryNormalizer.normalize("Résumé!")).isEqualTo("résumé");
    }
}