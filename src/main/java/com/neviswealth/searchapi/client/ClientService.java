package com.neviswealth.searchapi.client;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public ClientEntity create(String firstName, String lastName, String email, String description, List<String> socialLinks) {
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException("firstName must not be blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("lastName must not be blank");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (clientRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateEmailException(normalizedEmail);
        }

        ClientEntity entity = new ClientEntity();
        entity.setId(UUID.randomUUID());
        entity.setFirstName(firstName);
        entity.setLastName(lastName);
        entity.setEmail(normalizedEmail);
        entity.setDescription(description);
        entity.setSocialLinks(socialLinks == null ? List.of() : List.copyOf(socialLinks));
        entity.setCreatedAt(Instant.now());

        clientRepository.save(entity);
        return entity;
    }
}
