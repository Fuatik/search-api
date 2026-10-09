package com.neviswealth.searchapi.client;

import com.neviswealth.searchapi.search.ClientSearchResult;
import java.util.List;

public interface ClientSearchRepositoryCustom {

    List<ClientSearchResult> searchCandidates(String tsQuery, String rawQuery, int limit);
}