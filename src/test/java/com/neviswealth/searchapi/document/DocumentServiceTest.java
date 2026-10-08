package com.neviswealth.searchapi.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.neviswealth.searchapi.client.ClientEntity;
import com.neviswealth.searchapi.client.ClientNotFoundException;
import com.neviswealth.searchapi.client.ClientRepository;
import com.neviswealth.searchapi.embedding.EmbeddingProvider;
import com.neviswealth.searchapi.summary.SummaryProvider;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private EmbeddingProvider embeddingProvider;

    @Mock
    private SummaryProvider summaryProvider;

    @Captor
    private ArgumentCaptor<DocumentEntity> documentCaptor;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(documentRepository, clientRepository, embeddingProvider, summaryProvider);
    }

    @Test
    void blankTitleThrowsAndDoesNotSave() {
        UUID clientId = UUID.randomUUID();

        assertThatThrownBy(() -> documentService.create(clientId, "   ", "valid content"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("title must not be blank");

        verify(documentRepository, never()).save(any(DocumentEntity.class));
    }

    @Test
    void nullContentThrowsEmptyContentException() {
        UUID clientId = UUID.randomUUID();

        assertThatThrownBy(() -> documentService.create(clientId, "Title", null))
                .isInstanceOf(EmptyContentException.class)
                .hasMessage("content must not be empty after normalization");
    }

    @Test
    void blankContentAfterNormalizationThrowsEmptyContentException() {
        UUID clientId = UUID.randomUUID();

        assertThatThrownBy(() -> documentService.create(clientId, "Title", "   "))
                .isInstanceOf(EmptyContentException.class)
                .hasMessage("content must not be empty after normalization");
    }

    @Test
    void unknownClientThrowsClientNotFoundException() {
        UUID clientId = UUID.randomUUID();
        when(clientRepository.findById(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.create(clientId, "Title", "Some content"))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessage("Client not found: " + clientId);
    }

    @Test
    void validInputSavesEntityAndBuildsEmbeddingInputFromTitleAndContent() {
        UUID clientId = UUID.randomUUID();
        ClientEntity client = new ClientEntity();
        client.setId(clientId);
        when(clientRepository.findById(clientId)).thenReturn(Optional.of(client));
        when(summaryProvider.summarize("Some content")).thenReturn("Some summary.");

        float[] embedding = new float[384];
        embedding[0] = 0.42f;
        when(embeddingProvider.embed("Title Some content")).thenReturn(embedding);

        DocumentEntity created = documentService.create(clientId, "Title", "Some content");

        verify(embeddingProvider).embed(eq("Title Some content"));
        verify(documentRepository).save(documentCaptor.capture());

        DocumentEntity saved = documentCaptor.getValue();
        assertThat(saved.getSummary()).isNotNull();
        assertThat(saved.getEmbedding()).hasSize(384);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getId()).isNotNull();

        assertThat(created).isSameAs(saved);
    }
}
