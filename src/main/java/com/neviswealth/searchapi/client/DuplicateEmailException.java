package com.neviswealth.searchapi.client;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("Client with email already exists: " + email);
    }
}
