package com.neviswealth.searchapi.document;

import com.neviswealth.searchapi.search.DocumentSearchRepositoryCustom;
import com.neviswealth.searchapi.search.DocumentSearchResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class DocumentRepositoryImpl implements DocumentSearchRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    private static final String SEARCH_SQL = """
            WITH candidates AS (
              SELECT
                d.id,
                d.client_id,
                d.title,
                d.content,
                d.summary,
                d.created_at,
                ts_rank_cd(to_tsvector('simple', coalesce(d.title,'') || ' ' || coalesce(d.content,'')),
                           to_tsquery('simple', :tsQuery)) AS lexical_score,
                GREATEST(similarity(coalesce(d.title,''), :rawQuery),
                         similarity(coalesce(d.content,''), :rawQuery)) AS trigram_score,
                CASE
                  WHEN d.embedding IS NULL THEN 0.0
                  ELSE 1.0 - (d.embedding <=> CAST(:queryVector AS vector))
                END AS semantic_score
              FROM documents d
            )
            SELECT
              id, client_id, title, content, summary, created_at,
              lexical_score,
              trigram_score,
              semantic_score,
              (:wLexical * lexical_score + :wSemantic * semantic_score + :wTrigram * trigram_score) AS final_score
            FROM candidates
            WHERE lexical_score > 0
               OR trigram_score > 0.1
               OR semantic_score > 0.5
            ORDER BY final_score DESC, lexical_score DESC, created_at DESC, id ASC
            LIMIT :limit
            """;

    @Override
    public List<DocumentSearchResult> searchCandidates(
            String tsQuery,
            String rawQuery,
            String queryVector,
            int limit,
            float wLexical,
            float wSemantic,
            float wTrigram
    ) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(SEARCH_SQL)
                .setParameter("tsQuery", tsQuery)
                .setParameter("rawQuery", rawQuery)
                .setParameter("queryVector", queryVector)
                .setParameter("limit", limit)
                .setParameter("wLexical", wLexical)
                .setParameter("wSemantic", wSemantic)
                .setParameter("wTrigram", wTrigram)
                .getResultList();

        return rows.stream().map(this::mapRow).toList();
    }

    private DocumentSearchResult mapRow(Object[] row) {
        return new DocumentSearchResult(
                (UUID) row[0],
                (UUID) row[1],
                (String) row[2],
                (String) row[3],
                (String) row[4],
                toInstant(row[5]),
                toDouble(row[6]),
                toDouble(row[7]),
                toDouble(row[8]),
                toDouble(row[9])
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