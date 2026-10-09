package com.neviswealth.searchapi.search;

import java.time.Instant;
import java.util.UUID;

public record ClientSearchResult(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String description,
        Instant createdAt,
        double lexicalScore,
        double trigramScore,
        double finalScore
) implements SearchCandidateResult {
}