package com.neviswealth.searchapi.api.dto;

import com.neviswealth.searchapi.search.SearchResultPage;
import com.neviswealth.searchapi.search.ClientSearchResult;
import com.neviswealth.searchapi.search.DocumentSearchResult;
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
                resultPage.items().stream().map(item -> {
                    if (item instanceof DocumentSearchResult documentSearchResult) {
                        return SearchItemResponse.from(documentSearchResult);
                    }
                    if (item instanceof ClientSearchResult clientSearchResult) {
                        return SearchItemResponse.fromClient(clientSearchResult);
                    }
                    throw new IllegalArgumentException("Unsupported search result type: " + item.getClass());
                }).toList()
        );
    }
}
