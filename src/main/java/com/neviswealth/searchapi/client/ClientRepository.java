package com.neviswealth.searchapi.client;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<ClientEntity, UUID> {

    boolean existsByEmail(String email);
}
