package com.neviswealth.searchapi.api;

import com.neviswealth.searchapi.api.dto.ClientResponse;
import com.neviswealth.searchapi.api.dto.CreateClientRequest;
import com.neviswealth.searchapi.client.ClientEntity;
import com.neviswealth.searchapi.client.ClientService;
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
@RequestMapping("/api/v1/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody CreateClientRequest request) {
        ClientEntity created = clientService.create(
                request.first_name(),
                request.last_name(),
                request.email(),
                request.description(),
                request.social_links()
        );
        return ResponseEntity
                .created(URI.create("/api/v1/clients/" + created.getId()))
                .body(ClientResponse.from(created));
    }

    @GetMapping("/{id}")
    public ClientResponse getById(@PathVariable UUID id) {
        return ClientResponse.from(clientService.findById(id));
    }
}
