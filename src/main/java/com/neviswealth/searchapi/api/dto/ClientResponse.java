package com.neviswealth.searchapi.api.dto;

import com.neviswealth.searchapi.client.ClientEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClientResponse(
        UUID id,
        String first_name,
        String last_name,
        String email,
        String description,
        List<String> social_links,
        Instant created_at
) {
    public static ClientResponse from(ClientEntity entity) {
        return new ClientResponse(
                entity.getId(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getEmail(),
                entity.getDescription(),
                entity.getSocialLinks(),
                entity.getCreatedAt()
        );
    }
}
