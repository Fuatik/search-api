package com.neviswealth.searchapi.api.dto;

public record ScoreBreakdown(
        double lexical,
        double trigram,
        double semantic,
        double finalScore
) {
}
