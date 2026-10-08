package com.neviswealth.searchapi.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.neviswealth.searchapi.search.DocumentSearchResult;
import com.neviswealth.searchapi.search.SearchResultPage;
import com.neviswealth.searchapi.search.SearchService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SearchController.class)
@Import(GlobalExceptionHandler.class)
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @Test
    void getWithQueryReturnsSearchResponse() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        when(searchService.search("address proof", 0, 20)).thenReturn(new SearchResultPage(
                0,
                20,
                1,
                1,
                List.of(new DocumentSearchResult(
                        documentId,
                        clientId,
                        "Address Proof",
                        "Content",
                        "Summary",
                        Instant.parse("2025-01-01T00:00:00Z"),
                        0.2,
                        0.3,
                        0.7,
                        0.5
                ))
        ));

        mockMvc.perform(get("/api/v1/search").param("q", "address proof"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].documentId").value(documentId.toString()))
                .andExpect(jsonPath("$.items[0].clientId").value(clientId.toString()));
    }

    @Test
    void getWithBlankQueryReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWithoutQueryReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/search"))
                .andExpect(status().isBadRequest());
    }
}
