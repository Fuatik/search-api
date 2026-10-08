package com.neviswealth.searchapi.search;

import java.util.List;

public interface DocumentSearchRepositoryCustom {

    List<DocumentSearchResult> searchCandidates(
            String tsQuery,
            String rawQuery,
            String queryVector,
            int limit,
            float wLexical,
            float wSemantic,
            float wTrigram
    );
}
