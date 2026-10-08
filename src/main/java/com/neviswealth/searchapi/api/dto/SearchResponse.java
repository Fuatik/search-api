package com.neviswealth.searchapi.api.dto;

import com.neviswealth.searchapi.search.SearchResultPage;
import java.util.List;

public record SearchResponse(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<SearchItemResponse> items
) {
    public static SearchResponse from(SearchResultPage resultPage) {
        return new SearchResponse(
                resultPage.page(),
                resultPage.size(),
                resultPage.totalElements(),
                resultPage.totalPages(),
                resultPage.items().stream().map(SearchItemResponse::from).toList()
        );
    }
}
