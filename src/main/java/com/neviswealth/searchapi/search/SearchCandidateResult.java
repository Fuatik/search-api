package com.neviswealth.searchapi.search;

import java.time.Instant;

public interface SearchCandidateResult {
    double finalScore();

    Instant createdAt();
}