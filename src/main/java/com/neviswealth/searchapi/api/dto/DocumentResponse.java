package com.neviswealth.searchapi.api.dto;

import com.neviswealth.searchapi.document.DocumentEntity;
import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID client_id,
        String title,
        String content,
        String summary,
        Instant created_at
) {
    public static DocumentResponse from(DocumentEntity entity) {
        return new DocumentResponse(
                entity.getId(),
                entity.getClientId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getSummary(),
                entity.getCreatedAt()
        );
    }
}
