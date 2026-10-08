package com.neviswealth.searchapi.search;

import java.util.List;

public record SearchResultPage(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<DocumentSearchResult> items
) {
}