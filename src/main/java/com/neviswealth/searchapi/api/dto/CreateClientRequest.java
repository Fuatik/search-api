package com.neviswealth.searchapi.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record CreateClientRequest(
        @NotBlank String first_name,
        @NotBlank String last_name,
        @NotBlank @Email String email,
        String description,
        List<String> social_links
) {
}
