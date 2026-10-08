package com.neviswealth.searchapi.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.neviswealth.searchapi.client.ClientEntity;
import com.neviswealth.searchapi.client.ClientRepository;
import com.neviswealth.searchapi.config.AppSearchProperties;
import com.neviswealth.searchapi.document.DocumentEntity;
import com.neviswealth.searchapi.document.DocumentRepository;
import com.neviswealth.searchapi.embedding.EmbeddingProvider;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration")
class DocumentSearchRepositorySearchIT {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private EmbeddingProvider embeddingProvider;

    @Autowired
    private SynonymExpander synonymExpander;

    @Autowired
    private AppSearchProperties appSearchProperties;

    private UUID clientId;

    @BeforeEach
    void setUp() {
        documentRepository.deleteAll();
        clientRepository.deleteAll();

        clientId = UUID.randomUUID();
        ClientEntity client = new ClientEntity();
        client.setId(clientId);
        client.setFirstName("Jane");
        client.setLastName("Doe");
        client.setEmail("jane.doe+searchit@example.com");
        client.setDescription("Search IT client");
        client.setSocialLinks(List.of("https://example.com/jane"));
        client.setCreatedAt(Instant.now());
        clientRepository.save(client);

        saveDocument("Electric Utility Bill - May",
                "Monthly utility bill for 340 kWh. Reference: address proof. Customer: Jane Doe.");
        saveDocument("Bank Statement - June",
                "Bank statement for June. Monthly account summary. Address on file confirmed.");
        saveDocument("Passport",
                "Identity document. Issued by government. Photo and signature.");
    }

    @AfterEach
    void tearDown() {
        documentRepository.deleteAll();
        clientRepository.deleteAll();
    }

    @Test
    void searchCandidatesRanksUtilityBillFirstForAddressProofQuery() {
        String normalized = QueryNormalizer.normalize("address proof");
        List<String> expanded = synonymExpander.expand(normalized);
        String tsQuery = buildTsQuery(expanded);
        float[] qv = embeddingProvider.embed("address proof");
        String queryVector = vectorToString(qv);

        List<DocumentSearchResult> results = documentRepository.searchCandidates(
                tsQuery,
                normalized,
                queryVector,
                200,
                appSearchProperties.getWeights().getLexical(),
                appSearchProperties.getWeights().getSemantic(),
                appSearchProperties.getWeights().getTrigram()
        );

        String ranking = results.stream()
                .map(r -> r.title() + "=" + r.finalScore())
                .toList()
                .toString();

        assertThat(results)
                .as("Search results for 'address proof' (ranking: %s)", ranking)
                .isNotEmpty();

        assertThat(results.getFirst().title())
                .as("Top result for 'address proof' (ranking: %s)", ranking)
                .isEqualTo("Electric Utility Bill - May");

        assertThat(results.stream().map(DocumentSearchResult::title))
                .as("'Bank Statement' should appear in results (ranking: %s)", ranking)
                .anyMatch(title -> title.contains("Bank Statement"));

        assertThat(results.getFirst().title())
                .as("'Passport' must not rank first (ranking: %s)", ranking)
                .isNotEqualTo("Passport");
    }

    private void saveDocument(String title, String content) {
        DocumentEntity document = new DocumentEntity();
        document.setId(UUID.randomUUID());
        document.setClientId(clientId);
        document.setTitle(title);
        document.setContent(content);
        document.setSummary(null);
        document.setEmbedding(embeddingProvider.embed(title + " " + content));
        document.setCreatedAt(Instant.now());
        documentRepository.save(document);
    }

    private static String buildTsQuery(List<String> terms) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String term : terms) {
            String normalized = QueryNormalizer.normalize(term);
            if (normalized.isBlank()) {
                continue;
            }
            for (String token : normalized.split("\\s+")) {
                if (!token.isBlank()) {
                    tokens.add(token);
                }
            }
        }
        if (tokens.isEmpty()) {
            return "zzzzzz";
        }
        return String.join(" | ", tokens);
    }

    private static String vectorToString(float[] vector) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(vector[i]);
        }
        builder.append(']');
        return builder.toString();
    }
}