package com.neviswealth.searchapi.api.dto;

import com.neviswealth.searchapi.search.ClientSearchResult;
import com.neviswealth.searchapi.search.DocumentSearchResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SearchItemResponse(
        String type,
        UUID id,
        UUID documentId,
        UUID clientId,
        String title,
        String summary,
        ScoreBreakdown scores,
        List<String> matchReasons,
        Instant createdAt
) {
    public static SearchItemResponse from(DocumentSearchResult result) {
        List<String> reasons = new ArrayList<>();
        if (result.lexicalScore() > 0) {
            reasons.add("FTS_MATCH");
        }
        if (result.trigramScore() > 0.1) {
            reasons.add("TRIGRAM_MATCH");
        }
        if (result.semanticScore() > 0.5) {
            reasons.add("VECTOR_COSINE");
        }
        if (reasons.isEmpty()) {
            reasons.add("CANDIDATE");
        }

        return new SearchItemResponse(
                "DOCUMENT",
                result.id(),
                result.id(),
                result.clientId(),
                result.title(),
                result.summary(),
                new ScoreBreakdown(
                        result.lexicalScore(),
                        result.trigramScore(),
                        result.semanticScore(),
                        result.finalScore()
                ),
                List.copyOf(reasons),
                result.createdAt()
        );
    }

    public static SearchItemResponse fromClient(ClientSearchResult result) {
        List<String> reasons = new ArrayList<>();
        if (result.lexicalScore() > 0) {
            reasons.add("FTS_MATCH");
        }
        if (result.trigramScore() > 0.1) {
            reasons.add("TRIGRAM_MATCH");
        }
        if (reasons.isEmpty()) {
            reasons.add("CANDIDATE");
        }

        String firstName = result.firstName() == null ? "" : result.firstName().trim();
        String lastName = result.lastName() == null ? "" : result.lastName().trim();
        String title = (firstName + " " + lastName).trim();

        return new SearchItemResponse(
                "CLIENT",
                result.id(),
                null,
                null,
                title,
                result.description(),
                new ScoreBreakdown(
                        result.lexicalScore(),
                        result.trigramScore(),
                        0.0,
                        result.finalScore()
                ),
                List.copyOf(reasons),
                result.createdAt()
        );
    }
}
