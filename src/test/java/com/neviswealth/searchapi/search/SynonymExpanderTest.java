package com.neviswealth.searchapi.search;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SynonymExpanderTest {

    @Test
    void exactMatchReturnsQueryThenSynonymsInOrder() {
        SynonymExpander expander = new SynonymExpander(sampleSynonyms());

        assertThat(expander.expand("address proof"))
                .containsExactly("address proof", "utility bill", "bank statement", "residence certificate");
    }

    @Test
    void noMatchReturnsSingleElementOriginalQuery() {
        SynonymExpander expander = new SynonymExpander(sampleSynonyms());

        assertThat(expander.expand("unknown term")).containsExactly("unknown term");
    }

    @Test
    void inputIsExpectedToBeNormalized() {
        SynonymExpander expander = new SynonymExpander(sampleSynonyms());

        assertThat(expander.expand("Address Proof")).containsExactly("Address Proof");
    }

    private static Map<String, List<String>> sampleSynonyms() {
        Map<String, List<String>> synonyms = new LinkedHashMap<>();
        synonyms.put("address proof", List.of("utility bill", "bank statement", "residence certificate"));
        return synonyms;
    }
}