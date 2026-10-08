package com.neviswealth.searchapi.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neviswealth.searchapi.client.ClientEntity;
import com.neviswealth.searchapi.client.ClientNotFoundException;
import com.neviswealth.searchapi.client.ClientService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClientController.class)
@Import(GlobalExceptionHandler.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClientService clientService;

    @Test
    void postValidReturnsCreatedWithLocationAndBody() throws Exception {
        UUID id = UUID.randomUUID();
        ClientEntity entity = client(id);
        when(clientService.create(any(), any(), any(), any(), any())).thenReturn(entity);

        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestCreateClientRequest(
                                "Ada",
                                "Lovelace",
                                "ada@example.com",
                                "desc",
                                List.of("https://example.com")
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/clients/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("ada@example.com"));
    }

    @Test
    void postBlankEmailReturnsValidationProblem() throws Exception {
        mockMvc.perform(post("/api/v1/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TestCreateClientRequest(
                                "Ada",
                                "Lovelace",
                                " ",
                                "desc",
                                List.of()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));
    }

    @Test
    void getFoundReturnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        when(clientService.findById(id)).thenReturn(client(id));

        mockMvc.perform(get("/api/v1/clients/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getMissingReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(clientService.findById(id)).thenThrow(new ClientNotFoundException(id));

        mockMvc.perform(get("/api/v1/clients/{id}", id))
                .andExpect(status().isNotFound());
    }

    private static ClientEntity client(UUID id) {
        ClientEntity entity = new ClientEntity();
        entity.setId(id);
        entity.setFirstName("Ada");
        entity.setLastName("Lovelace");
        entity.setEmail("ada@example.com");
        entity.setDescription("desc");
        entity.setSocialLinks(List.of("https://example.com"));
        entity.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        return entity;
    }

    private record TestCreateClientRequest(
            String first_name,
            String last_name,
            String email,
            String description,
            List<String> social_links
    ) {
    }
}
