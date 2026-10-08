package com.neviswealth.searchapi.api;

import com.neviswealth.searchapi.api.dto.CreateDocumentRequest;
import com.neviswealth.searchapi.api.dto.DocumentResponse;
import com.neviswealth.searchapi.document.DocumentEntity;
import com.neviswealth.searchapi.document.DocumentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping("/clients/{clientId}/documents")
    public ResponseEntity<DocumentResponse> create(
            @PathVariable UUID clientId,
            @Valid @RequestBody CreateDocumentRequest request
    ) {
        DocumentEntity created = documentService.create(clientId, request.title(), request.content());
        return ResponseEntity
                .created(URI.create("/api/v1/documents/" + created.getId()))
                .body(DocumentResponse.from(created));
    }

    @GetMapping("/documents/{id}")
    public DocumentResponse getById(@PathVariable UUID id) {
        return DocumentResponse.from(documentService.findById(id));
    }
}
