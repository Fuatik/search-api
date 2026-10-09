package com.neviswealth.searchapi.search;

import com.neviswealth.searchapi.client.ClientRepository;
import com.neviswealth.searchapi.config.AppSearchProperties;
import com.neviswealth.searchapi.document.DocumentRepository;
import com.neviswealth.searchapi.embedding.EmbeddingProvider;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SearchService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int CANDIDATE_LIMIT = 200;

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final EmbeddingProvider embeddingProvider;
    private final SynonymExpander synonymExpander;
    private final AppSearchProperties appSearchProperties;

    public SearchService(
            DocumentRepository documentRepository,
            ClientRepository clientRepository,
            EmbeddingProvider embeddingProvider,
            SynonymExpander synonymExpander,
            AppSearchProperties appSearchProperties
    ) {
        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
        this.embeddingProvider = embeddingProvider;
        this.synonymExpander = synonymExpander;
        this.appSearchProperties = appSearchProperties;
    }

    public SearchResultPage search(String rawQuery, int page, int size) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return new SearchResultPage(page, size, 0, 0, List.of());
        }

        int safePage = Math.max(page, 0);

        int safeSize = size;
        if (safeSize < 1) {
            safeSize = appSearchProperties.getPageSizeDefault();
        }
        safeSize = Math.min(safeSize, MAX_PAGE_SIZE);

        String normalized = QueryNormalizer.normalize(rawQuery);
        List<String> expandedTerms = synonymExpander.expand(normalized);
        String tsQuery = buildTsQuery(expandedTerms);

        float[] queryEmbedding = embeddingProvider.embed(normalized);
        String queryVector = vectorToString(queryEmbedding);

        List<DocumentSearchResult> candidates = documentRepository.searchCandidates(
                tsQuery,
                normalized,
                queryVector,
                CANDIDATE_LIMIT,
                appSearchProperties.getWeights().getLexical(),
                appSearchProperties.getWeights().getSemantic(),
                appSearchProperties.getWeights().getTrigram()
        );
        List<ClientSearchResult> clientCandidates = clientRepository.searchCandidates(tsQuery, normalized, CANDIDATE_LIMIT);

        List<SearchCandidateResult> merged = java.util.stream.Stream.concat(candidates.stream(), clientCandidates.stream())
                .map(SearchCandidateResult.class::cast)
                .sorted(Comparator.comparingDouble(SearchCandidateResult::finalScore).reversed())
                .toList();

        int from = Math.min(safePage * safeSize, merged.size());
        int to = Math.min(from + safeSize, merged.size());
        List<SearchCandidateResult> pageItems = List.copyOf(merged.subList(from, to));

        long totalElements = merged.size();
        int totalPages = (int) ((totalElements + safeSize - 1) / safeSize);

        return new SearchResultPage(safePage, safeSize, totalElements, totalPages, pageItems);
    }

    private static String buildTsQuery(List<String> terms) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String term : terms) {
            String normalized = QueryNormalizer.normalize(term);
            if (normalized.isBlank()) {
                continue;
            }
            for (String token : normalized.split("\\s+")) {
                if (!token.isBlank()) {
                    tokens.add(token);
                }
            }
        }
        if (tokens.isEmpty()) {
            return "zzzzzz";
        }
        return String.join(" | ", tokens);
    }

    private static String vectorToString(float[] vector) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(vector[i]);
        }
        builder.append(']');
        return builder.toString();
    }
}