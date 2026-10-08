package com.neviswealth.searchapi.client;

import java.util.UUID;

public class ClientNotFoundException extends RuntimeException {

    public ClientNotFoundException(UUID clientId) {
        super("Client not found: " + clientId);
    }
}
