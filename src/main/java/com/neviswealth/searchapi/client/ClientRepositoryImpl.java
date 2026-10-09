package com.neviswealth.searchapi.client;

import com.neviswealth.searchapi.search.ClientSearchResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ClientRepositoryImpl implements ClientSearchRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final String SEARCH_SQL = """
            WITH candidates AS (
              SELECT c.id, c.first_name, c.last_name, c.email, c.description, c.created_at,
                     ts_rank_cd(to_tsvector('simple', coalesce(c.first_name,'') || ' ' || coalesce(c.last_name,'') || ' ' || coalesce(c.email,'') || ' ' || coalesce(c.description,'')), to_tsquery('simple', :tsQuery)) AS lexical_score,
                     GREATEST(similarity(coalesce(c.first_name,''), :rawQuery),
                              similarity(coalesce(c.last_name,''), :rawQuery),
                              similarity(coalesce(c.email,''), :rawQuery),
                              similarity(coalesce(c.description,''), :rawQuery)) AS trigram_score
              FROM clients c
            )
            SELECT id, first_name, last_name, email, description, created_at, lexical_score, trigram_score,
                   (0.7 * lexical_score + 0.3 * trigram_score) AS final_score
            FROM candidates
            WHERE lexical_score > 0 OR trigram_score > 0.1
            ORDER BY final_score DESC, lexical_score DESC, created_at DESC, id
            LIMIT :limit
            """;

    @Override
    public List<ClientSearchResult> searchCandidates(String tsQuery, String rawQuery, int limit) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(SEARCH_SQL)
                .setParameter("tsQuery", tsQuery)
                .setParameter("rawQuery", rawQuery)
                .setParameter("limit", limit)
                .getResultList();

        return rows.stream().map(this::mapRow).toList();
    }

    private ClientSearchResult mapRow(Object[] row) {
        return new ClientSearchResult(
                (UUID) row[0],
                (String) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                toInstant(row[5]),
                toDouble(row[6]),
                toDouble(row[7]),
                toDouble(row[8])
        );
    }

    private static Instant toInstant(Object value) {
        if (value instanceof Instant instant) return instant;
        if (value instanceof Timestamp timestamp) return timestamp.toInstant();
        throw new IllegalArgumentException("Unsupported created_at value type: " + value);
    }

    private static double toDouble(Object value) {
        if (value instanceof Number number) return number.doubleValue();
        throw new IllegalArgumentException("Unsupported numeric value type: " + value);
    }
}