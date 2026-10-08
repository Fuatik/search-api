package com.neviswealth.searchapi.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.neviswealth.searchapi.client.ClientEntity;
import com.neviswealth.searchapi.client.ClientRepository;
import com.neviswealth.searchapi.client.ClientService;
import com.neviswealth.searchapi.document.DocumentRepository;
import com.neviswealth.searchapi.document.DocumentService;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration acceptance test for the assignment's core semantic ranking behavior.
 *
 * <p>This verifies that for an "address proof" query, a clearly relevant utility bill ranks above
 * less relevant documents in the end-to-end search flow (synonym expansion + FTS + trigram +
 * pgvector cosine).
 *
 * <p>The test asserts on {@link DocumentSearchResult} — what {@link SearchService} returns — not on
 * the controller-layer DTO. Match-reason formatting is covered in {@code SearchControllerTest}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("integration")
class SearchRankingAcceptanceIT {

    @Autowired
    private ClientService clientService;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        clientRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        documentRepository.deleteAll();
        clientRepository.deleteAll();
    }

    @Test
    void shouldRankUtilityBillAboveOthersForAddressProofQuery() {
        ClientEntity client = clientService.create(
                "John",
                "Doe",
                "john.doe@neviswealth.com",
                "desc",
                List.of()
        );

        documentService.create(
                client.getId(),
                "Electric Utility Bill - May",
                "Monthly utility bill for 340 kWh. Reference: address proof. Customer: John Doe."
        );
        documentService.create(
                client.getId(),
                "Bank Statement - June",
                "Bank statement for June. Monthly account summary. Address on file confirmed."
        );
        documentService.create(
                client.getId(),
                "Passport",
                "Identity document. Issued by government. Photo and signature."
        );

        SearchResultPage page = searchService.search("address proof", 0, 20);

        String ranking = page.items().stream()
                .map(r -> r.title()
                        + "[final=" + r.finalScore()
                        + ",lexical=" + r.lexicalScore()
                        + ",semantic=" + r.semanticScore()
                        + ",trigram=" + r.trigramScore()
                        + "]")
                .toList()
                .toString();

        assertThat(page.totalElements())
                .as("Search results for 'address proof' (ranking: %s)", ranking)
                .isGreaterThanOrEqualTo(1);

        assertThat(page.items())
                .as("Search results for 'address proof' (ranking: %s)", ranking)
                .isNotEmpty();

        DocumentSearchResult top = page.items().getFirst();

        assertThat(top.title())
                .as("Top result for 'address proof' (ranking: %s)", ranking)
                .isEqualTo("Electric Utility Bill - May");

        assertThat(top.finalScore())
                .as("Top result finalScore must be > 0 (ranking: %s)", ranking)
                .isGreaterThan(0.0);

        assertThat(top.lexicalScore())
                .as("Top result lexicalScore must be > 0 (ranking: %s)", ranking)
                .isGreaterThan(0.0);

        assertThat(page.items().getFirst().title())
                .as("'Passport' must not rank first (ranking: %s)", ranking)
                .isNotEqualTo("Passport");
    }
}