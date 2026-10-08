package com.neviswealth.searchapi.search;

import java.time.Instant;
import java.util.UUID;

public record DocumentSearchResult(
        UUID id,
        UUID clientId,
        String title,
        String content,
        String summary,
        Instant createdAt,
        double lexicalScore,
        double trigramScore,
        double semanticScore,
        double finalScore
) {
}
