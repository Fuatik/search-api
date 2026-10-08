package com.neviswealth.searchapi.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.neviswealth.searchapi.config.AppSearchProperties;
import com.neviswealth.searchapi.document.DocumentRepository;
import com.neviswealth.searchapi.embedding.EmbeddingProvider;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private EmbeddingProvider embeddingProvider;

    @Captor
    private ArgumentCaptor<String> queryVectorCaptor;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        AppSearchProperties properties = new AppSearchProperties();
        Map<String, List<String>> synonyms = new LinkedHashMap<>();
        synonyms.put("address proof", List.of("utility bill"));
        SynonymExpander synonymExpander = new SynonymExpander(synonyms);
        searchService = new SearchService(documentRepository, embeddingProvider, synonymExpander, properties);
    }

    @Test
    void nullQueryReturnsEmptyPageAndDoesNotCallRepository() {
        SearchResultPage result = searchService.search(null, 0, 10);

        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
        assertThat(result.items()).isEmpty();
        verify(documentRepository, never()).searchCandidates(
                eq(""),
                eq(""),
                eq(""),
                eq(0),
                anyFloat(),
                anyFloat(),
                anyFloat()
        );
    }

    @Test
    void blankQueryReturnsEmptyPage() {
        SearchResultPage result = searchService.search("   ", 1, 5);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
        assertThat(result.items()).isEmpty();
        verify(documentRepository, never()).searchCandidates(
                eq(""),
                eq(""),
                eq(""),
                eq(0),
                anyFloat(),
                anyFloat(),
                anyFloat()
        );
    }

    @Test
    void normalQueryCallsRepositoryAndPaginatesInMemory() {
        when(embeddingProvider.embed("address proof")).thenReturn(new float[]{0.1f, 0.2f});
        when(documentRepository.searchCandidates(
                eq("address | proof | utility | bill"),
                eq("address proof"),
                queryVectorCaptor.capture(),
                eq(200),
                anyFloat(),
                anyFloat(),
                anyFloat()
        )).thenReturn(sampleResults());

        SearchResultPage result = searchService.search("Address Proof!", 0, 2);

        verify(documentRepository).searchCandidates(
                eq("address | proof | utility | bill"),
                eq("address proof"),
                queryVectorCaptor.capture(),
                eq(200),
                anyFloat(),
                anyFloat(),
                anyFloat()
        );
        assertThat(queryVectorCaptor.getValue()).startsWith("[").endsWith("]");
        assertThat(result.items()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void firstPageWithSizeTwoReturnsTwoItems() {
        when(embeddingProvider.embed("address proof")).thenReturn(new float[]{1.0f});
        when(documentRepository.searchCandidates(
                eq("address | proof | utility | bill"),
                eq("address proof"),
                eq("[1.0]"),
                eq(200),
                anyFloat(),
                anyFloat(),
                anyFloat()
        )).thenReturn(sampleResults());

        SearchResultPage result = searchService.search("address proof", 0, 2);

        assertThat(result.items()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
    }

    @Test
    void secondPageWithSizeTwoReturnsOneItem() {
        when(embeddingProvider.embed("address proof")).thenReturn(new float[]{1.0f});
        when(documentRepository.searchCandidates(
                eq("address | proof | utility | bill"),
                eq("address proof"),
                eq("[1.0]"),
                eq(200),
                anyFloat(),
                anyFloat(),
                anyFloat()
        )).thenReturn(sampleResults());

        SearchResultPage result = searchService.search("address proof", 1, 2);

        assertThat(result.items()).hasSize(1);
    }

    @Test
    void sizeLargerThanHundredIsClampedToHundred() {
        when(embeddingProvider.embed("address proof")).thenReturn(new float[]{1.0f});
        when(documentRepository.searchCandidates(
                eq("address | proof | utility | bill"),
                eq("address proof"),
                eq("[1.0]"),
                eq(200),
                anyFloat(),
                anyFloat(),
                anyFloat()
        )).thenReturn(sampleResults());

        SearchResultPage result = searchService.search("address proof", 0, 1000);

        assertThat(result.size()).isEqualTo(100);
    }

    @Test
    void negativePageIsTreatedAsZero() {
        when(embeddingProvider.embed("address proof")).thenReturn(new float[]{1.0f});
        when(documentRepository.searchCandidates(
                eq("address | proof | utility | bill"),
                eq("address proof"),
                eq("[1.0]"),
                eq(200),
                anyFloat(),
                anyFloat(),
                anyFloat()
        )).thenReturn(sampleResults());

        SearchResultPage result = searchService.search("address proof", -1, 2);

        assertThat(result.page()).isZero();
        assertThat(result.items()).hasSize(2);
    }

    private static List<DocumentSearchResult> sampleResults() {
        UUID clientId = UUID.randomUUID();
        Instant now = Instant.now();
        return List.of(
                new DocumentSearchResult(UUID.randomUUID(), clientId, "First", "A", null, now, 0.9, 0.8, 0.7, 0.95),
                new DocumentSearchResult(UUID.randomUUID(), clientId, "Second", "B", null, now, 0.8, 0.7, 0.6, 0.85),
                new DocumentSearchResult(UUID.randomUUID(), clientId, "Third", "C", null, now, 0.7, 0.6, 0.5, 0.75)
        );
    }
}