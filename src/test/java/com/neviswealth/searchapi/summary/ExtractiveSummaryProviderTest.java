package com.neviswealth.searchapi.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.neviswealth.searchapi.config.AppSummaryProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExtractiveSummaryProviderTest {

    private ExtractiveSummaryProvider provider;

    @BeforeEach
    void setUp() {
        AppSummaryProperties props = new AppSummaryProperties();
        props.getExtractive().setMinSentences(2);
        props.getExtractive().setMaxSentences(3);
        provider = new ExtractiveSummaryProvider(props);
    }

    @Test
    void nullContentReturnsEmptyString() {
        assertThat(provider.summarize(null)).isEmpty();
    }

    @Test
    void blankContentReturnsEmptyString() {
        assertThat(provider.summarize("   ")).isEmpty();
    }

    @Test
    void singleSentenceReturnsTrimmedInput() {
        assertThat(provider.summarize("  One simple sentence for summary.  ")).isEqualTo("One simple sentence for summary.");
    }

    @Test
    void fiveSentencesSummaryHasAtMostConfiguredMaxSentences() {
        String content = "Alpha explores market trends. Beta tracks client outcomes. Gamma improves wealth planning. "
                + "Delta measures portfolio performance. Epsilon compares risk profiles.";

        String summary = provider.summarize(content);
        int sentenceCount = countSentences(summary);

        assertThat(sentenceCount).isLessThanOrEqualTo(3);
    }

    @Test
    void summarizeIsDeterministicForSameInput() {
        String content = "Finance models adapt quickly. Teams review models weekly. Models improve client experience.";

        String first = provider.summarize(content);
        String second = provider.summarize(content);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void nonAsciiInputDoesNotThrow() {
        String content = "İstanbul büyüyor. Zürich wächst schnell. São Paulo muda rápido. 東京は大きい。";

        assertThatCode(() -> provider.summarize(content)).doesNotThrowAnyException();
    }

    private static int countSentences(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String sentence : text.split("[.!?]+")) {
            if (!sentence.trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }
}
