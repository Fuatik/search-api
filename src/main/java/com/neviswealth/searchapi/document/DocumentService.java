package com.neviswealth.searchapi.document;

import com.neviswealth.searchapi.client.ClientNotFoundException;
import com.neviswealth.searchapi.client.ClientRepository;
import com.neviswealth.searchapi.embedding.EmbeddingProvider;
import com.neviswealth.searchapi.search.QueryNormalizer;
import com.neviswealth.searchapi.summary.SummaryProvider;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ClientRepository clientRepository;
    private final EmbeddingProvider embeddingProvider;
    private final SummaryProvider summaryProvider;

    public DocumentService(
            DocumentRepository documentRepository,
            ClientRepository clientRepository,
            EmbeddingProvider embeddingProvider,
            SummaryProvider summaryProvider
    ) {
        this.documentRepository = documentRepository;
        this.clientRepository = clientRepository;
        this.embeddingProvider = embeddingProvider;
        this.summaryProvider = summaryProvider;
    }

    public DocumentEntity create(UUID clientId, String title, String content) {
        if (clientId == null) {
            throw new IllegalArgumentException("clientId must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (content == null || QueryNormalizer.normalize(content).isEmpty()) {
            throw new EmptyContentException("content must not be empty after normalization");
        }
        if (clientRepository.findById(clientId).isEmpty()) {
            throw new ClientNotFoundException(clientId);
        }

        String summary = summaryProvider.summarize(content);
        float[] embedding = embeddingProvider.embed(title + " " + content);

        DocumentEntity entity = new DocumentEntity();
        entity.setId(UUID.randomUUID());
        entity.setClientId(clientId);
        entity.setTitle(title);
        entity.setContent(content);
        entity.setSummary(summary);
        entity.setEmbedding(embedding);
        entity.setCreatedAt(Instant.now());

        documentRepository.save(entity);
        return entity;
    }
}
