package com.neviswealth.searchapi.document;

import com.neviswealth.searchapi.search.DocumentSearchRepositoryCustom;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID>, DocumentSearchRepositoryCustom {
}
