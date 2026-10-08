package com.neviswealth.searchapi.api;

import com.neviswealth.searchapi.api.dto.SearchResponse;
import com.neviswealth.searchapi.api.exception.InvalidQueryException;
import com.neviswealth.searchapi.search.SearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchResponse search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        if (q.isBlank()) {
            throw new InvalidQueryException("q must not be blank");
        }
        return SearchResponse.from(searchService.search(q, page, size));
    }
}
