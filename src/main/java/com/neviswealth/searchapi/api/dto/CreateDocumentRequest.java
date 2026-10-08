package com.neviswealth.searchapi.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDocumentRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}
