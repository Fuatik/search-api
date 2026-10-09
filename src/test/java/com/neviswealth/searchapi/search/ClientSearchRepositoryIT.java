package com.neviswealth.searchapi.search;

import static org.assertj.core.api.Assertions.assertThat;

import com.neviswealth.searchapi.client.ClientEntity;
import com.neviswealth.searchapi.client.ClientRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("integration")
class ClientSearchRepositoryIT {

    @Autowired
    private ClientRepository clientRepository;

    @BeforeEach
    void setUp() {
        clientRepository.deleteAll();

        saveClient("John", "Doe", "john.doe@neviswealth.com", "Senior advisor at Nevis Wealth");
        saveClient("Jane", "Smith", "jane.smith@example.com", "Portfolio manager");
    }

    @AfterEach
    void tearDown() {
        clientRepository.deleteAll();
    }

    @Test
    void queryNevisWealthRanksJohnDoeFirst() {
        List<ClientSearchResult> results = clientRepository.searchCandidates("neviswealth", "neviswealth", 200);

        assertThat(results).isNotEmpty();
        assertThat(results.getFirst().email()).isEqualTo("john.doe@neviswealth.com");
    }

    @Test
    void queryJaneSmithFindsJane() {
        List<ClientSearchResult> results = clientRepository.searchCandidates("jane | smith", "jane smith", 200);

        assertThat(results).extracting(ClientSearchResult::email).contains("jane.smith@example.com");
    }

    @Test
    void queryUnknownReturnsEmpty() {
        List<ClientSearchResult> results = clientRepository.searchCandidates("unknownterm", "unknownterm", 200);

        assertThat(results).isEmpty();
    }

    private void saveClient(String firstName, String lastName, String email, String description) {
        ClientEntity client = new ClientEntity();
        client.setId(UUID.randomUUID());
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setEmail(email);
        client.setDescription(description);
        client.setSocialLinks(List.of());
        client.setCreatedAt(Instant.now());
        clientRepository.save(client);
    }
}