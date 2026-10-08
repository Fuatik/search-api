package com.neviswealth.searchapi.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neviswealth.searchapi.api.exception.ResourceNotFoundException;
import com.neviswealth.searchapi.client.ClientNotFoundException;
import com.neviswealth.searchapi.document.DocumentEntity;
import com.neviswealth.searchapi.document.DocumentService;
import com.neviswealth.searchapi.document.EmptyContentException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DocumentController.class)
@Import(GlobalExceptionHandler.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DocumentService documentService;

    @Test
    void postValidReturnsCreated() throws Exception {
        UUID clientId = UUID.randomUUID();
        UUID docId = UUID.randomUUID();
        when(documentService.create(eq(clientId), anyString(), anyString())).thenReturn(document(docId, clientId));

        mockMvc.perform(post("/api/v1/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestCreateDocumentRequest("Title", "Content"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/documents/" + docId));
    }

    @Test
    void postBlankContentReturnsValidationError() throws Exception {
        UUID clientId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestCreateDocumentRequest("Title", "   "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void postServiceEmptyContentReturnsUnprocessableEntity() throws Exception {
        UUID clientId = UUID.randomUUID();
        when(documentService.create(eq(clientId), anyString(), anyString()))
                .thenThrow(new EmptyContentException("content must not be empty after normalization"));

        mockMvc.perform(post("/api/v1/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestCreateDocumentRequest("Title", "Content"))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void postServiceClientMissingReturnsNotFound() throws Exception {
        UUID clientId = UUID.randomUUID();
        when(documentService.create(eq(clientId), anyString(), anyString()))
                .thenThrow(new ClientNotFoundException(clientId));

        mockMvc.perform(post("/api/v1/clients/{clientId}/documents", clientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestCreateDocumentRequest("Title", "Content"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMissingReturnsNotFound() throws Exception {
        UUID docId = UUID.randomUUID();
        when(documentService.findById(docId)).thenThrow(new ResourceNotFoundException("Document", docId));

        mockMvc.perform(get("/api/v1/documents/{id}", docId))
                .andExpect(status().isNotFound());
    }

    private static DocumentEntity document(UUID id, UUID clientId) {
        DocumentEntity entity = new DocumentEntity();
        entity.setId(id);
        entity.setClientId(clientId);
        entity.setTitle("Title");
        entity.setContent("Content");
        entity.setSummary("Summary");
        entity.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        return entity;
    }

    private record TestCreateDocumentRequest(String title, String content) {
    }
}
