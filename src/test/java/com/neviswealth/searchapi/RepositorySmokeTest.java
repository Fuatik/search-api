package com.neviswealth.searchapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.neviswealth.searchapi.client.ClientEntity;
import com.neviswealth.searchapi.client.ClientRepository;
import com.neviswealth.searchapi.document.DocumentEntity;
import com.neviswealth.searchapi.document.DocumentRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RepositorySmokeTest {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveAndReadClientAndDocument() {
        UUID clientId = UUID.randomUUID();
        Instant clientCreatedAt = Instant.now();

        ClientEntity client = new ClientEntity();
        client.setId(clientId);
        client.setFirstName("Jane");
        client.setLastName("Doe");
        client.setEmail("jane.doe+smoke@example.com");
        client.setDescription("Smoke test client");
        client.setSocialLinks(List.of("https://example.com"));
        client.setCreatedAt(clientCreatedAt);
        clientRepository.save(client);

        UUID documentId = UUID.randomUUID();
        Instant documentCreatedAt = Instant.now();
        float[] embedding = new float[384];
        Arrays.fill(embedding, 0.1f);

        DocumentEntity document = new DocumentEntity();
        document.setId(documentId);
        document.setClientId(clientId);
        document.setTitle("Test title");
        document.setContent("Test content");
        document.setSummary(null);
        document.setEmbedding(embedding);
        document.setCreatedAt(documentCreatedAt);
        documentRepository.save(document);

        entityManager.flush();
        entityManager.clear();

        ClientEntity loadedClient = clientRepository.findById(clientId).orElseThrow();
        DocumentEntity loadedDocument = documentRepository.findById(documentId).orElseThrow();

        assertThat(loadedClient.getId()).isEqualTo(clientId);
        assertThat(loadedClient.getFirstName()).isEqualTo("Jane");
        assertThat(loadedClient.getLastName()).isEqualTo("Doe");
        assertThat(loadedClient.getEmail()).isEqualTo("jane.doe+smoke@example.com");
        assertThat(loadedClient.getDescription()).isEqualTo("Smoke test client");
        assertThat(loadedClient.getSocialLinks()).hasSize(1);
        assertThat(loadedClient.getSocialLinks().getFirst()).isEqualTo("https://example.com");

        assertThat(loadedDocument.getId()).isEqualTo(documentId);
        assertThat(loadedDocument.getClientId()).isEqualTo(clientId);
        assertThat(loadedDocument.getTitle()).isEqualTo("Test title");
        assertThat(loadedDocument.getContent()).isEqualTo("Test content");
        assertThat(loadedDocument.getSummary()).isNull();
        assertThat(loadedDocument.getEmbedding()).hasSize(384);
        assertThat(loadedDocument.getEmbedding()[0]).isCloseTo(0.1f, within(0.00001f));
        for (float value : loadedDocument.getEmbedding()) {
            assertThat(value).isCloseTo(0.1f, within(0.00001f));
        }
        assertThat(loadedDocument.getCreatedAt()).isEqualTo(documentCreatedAt);
    }
}
