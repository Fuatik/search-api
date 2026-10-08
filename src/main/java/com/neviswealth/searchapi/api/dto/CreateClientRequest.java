package com.neviswealth.searchapi.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record CreateClientRequest(
        @NotBlank
        @Pattern(regexp = "^[\\p{L}\\p{M}\\s\\-'.]+$", message = "must contain only letters, spaces, hyphens, apostrophes")
        String first_name,

        @NotBlank
        @Pattern(regexp = "^[\\p{L}\\p{M}\\s\\-'.]+$", message = "must contain only letters, spaces, hyphens, apostrophes")
        String last_name,

        @NotBlank @Email String email,
        String description,
        List<String> social_links
) {
}
