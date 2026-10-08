# Nevis Search API

Nevis Search API is a Java/Spring Boot home assignment that provides client/document management and ranked semantic-lexical document search. It combines deterministic embeddings, PostgreSQL full-text search, trigram similarity, and pgvector cosine distance in a reproducible offline pipeline. The project is designed for local execution with Dockerized Postgres and Maven Wrapper.

## Requirements

- JDK `21` (LTS) (verified baseline).
- Docker (for PostgreSQL + pgvector via `docker compose`).
- Maven Wrapper (`./mvnw`, no global Maven required).

## Quick start

```bash
docker compose up -d
./mvnw spring-boot:run
```

Health check (application responding):

```bash
curl -i "http://localhost:8080/v3/api-docs"
```

Swagger UI:

- `http://localhost:8080/swagger-ui.html`

## API

| Method | Path | Status codes |
|---|---|---|
| POST | `/api/v1/clients` | `201`, `400`, `409`, `422`, `500` |
| GET | `/api/v1/clients/{id}` | `200`, `400`, `404`, `500` |
| POST | `/api/v1/clients/{id}/documents` | `201`, `400`, `404`, `422`, `500` |
| GET | `/api/v1/documents/{id}` | `200`, `400`, `404`, `500` |
| GET | `/api/v1/search?q=&page=&size=` | `200`, `400`, `500` |

Error responses use RFC7807 `ProblemDetail`.

## Example: semantic search

This walkthrough shows how `address proof` returns `Electric Utility Bill - May` at the top. Ranking is produced by synonym expansion + FTS + trigram + pgvector cosine, then merged with weighted scoring.

Create a client:

```bash
curl -X POST "http://localhost:8080/api/v1/clients" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com"
  }'
```

Add document 1:

```bash
curl -X POST "http://localhost:8080/api/v1/clients/1d46e4f9-cf9a-40a7-a56a-906cbd187dfc/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Electric Utility Bill - May",
    "content": "Monthly utility bill for 340 kWh. Reference: address proof. Customer: John Doe."
  }'
```

Add document 2:

```bash
curl -X POST "http://localhost:8080/api/v1/clients/1d46e4f9-cf9a-40a7-a56a-906cbd187dfc/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Bank Statement - June",
    "content": "Bank statement for June. Monthly account summary. Address on file confirmed."
  }'
```

Add document 3:

```bash
curl -X POST "http://localhost:8080/api/v1/clients/1d46e4f9-cf9a-40a7-a56a-906cbd187dfc/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Travel Itinerary",
    "content": "Flight and hotel booking details for business travel."
  }'
```

Run search:

```bash
curl "http://localhost:8080/api/v1/search?q=address+proof"
```

```json
{
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1,
  "items": [
    {
      "documentId": "c535d3d3-040f-4101-93c9-a204e2cf30db",
      "clientId": "1d46e4f9-cf9a-40a7-a56a-906cbd187dfc",
      "title": "Electric Utility Bill - May",
      "summary": "Monthly utility bill for 340 kWh. Reference: address proof. Customer: John Doe.",
      "scores": {
        "lexical": 0.6000000238418579,
        "trigram": 0.18666666746139526,
        "semantic": 0.31622778273986474,
        "finalScore": 0.43220168624007055
      },
      "matchReasons": ["FTS_MATCH", "TRIGRAM_MATCH"],
      "createdAt": "2026-10-08T21:53:50.672837Z"
    },
    {
      "documentId": "8c88efd1-103c-4fda-a81d-e0a5dec1e252",
      "clientId": "1d46e4f9-cf9a-40a7-a56a-906cbd187dfc",
      "title": "Bank Statement - June",
      "summary": "Bank statement for June. Monthly account summary. Address on file confirmed.",
      "scores": {
        "lexical": 0.5,
        "trigram": 0.10526315867900848,
        "semantic": 0.15811389136993237,
        "finalScore": 0.3184868017767039
      },
      "matchReasons": ["FTS_MATCH", "TRIGRAM_MATCH"],
      "createdAt": "2026-10-08T21:53:56.548363Z"
    }
  ]
}
```

## Architecture

- API layer (`com.neviswealth.searchapi.api`): REST controllers and RFC7807 error mapping.
- Service layer: client/document/search orchestration.
- Search pipeline:
  - `QueryNormalizer`
  - `SynonymExpander` (YAML-driven)
  - native SQL with `ts_rank_cd`, `pg_trgm` similarity, `pgvector` cosine (`<=>`)
  - Java weighted merge (`lexical=0.5`, `semantic=0.3`, `trigram=0.2`)
  - in-memory pagination
- Embeddings: deterministic hashing provider (`com.neviswealth.searchapi.embedding.HashingEmbeddingProvider`), `384` dimensions, no external APIs.
- Summary: deterministic extractive provider (`com.neviswealth.searchapi.summary.ExtractiveSummaryProvider`), offline pure Java.
- Persistence: PostgreSQL 16 + pgvector (`pgvector/pgvector:pg16`), Liquibase-managed schema (`clients`, `documents`), extensions `vector` + `pg_trgm`.
- Document indexes:
  - HNSW on embedding (`vector_cosine_ops`)
  - GIN trigram on `title`
  - GIN trigram on `content`
  - btree on `client_id`
  - btree on `created_at`
- Embedding column mapping: `hibernate-vector 6.6.13.Final` with `@JdbcTypeCode(SqlTypes.VECTOR)` + `@Array(length = 384)`.
- Tie-break order: `finalScore DESC` → `lexicalScore DESC` → `createdAt DESC` → `documentId ASC`.

## Design decisions and trade-offs

### Why hashing embeddings instead of sentence-transformers / ONNX / Python sidecar

Hashing embeddings keep the MVP fully offline, deterministic, and Java-only. This removes model distribution/runtime complexity and external service dependencies, while still enabling vector search behavior for the assignment.

### Why no LLM-based query expansion

LLM expansion would add non-determinism, cost, and infrastructure/runtime variability. The current YAML synonym approach is predictable, testable, and CI-stable.

### Why extractive summary is the default

Extractive summaries are deterministic, offline, and require no external API. This keeps response generation reproducible and environment-independent.

### Optional: Ollama-backed summary

An Ollama-backed summary provider is available as an opt-in path behind `app.summary.provider=ollama`. It requires a running Ollama instance with the `llama3.2:1b` model pulled (approximately `1.3 GB`). The default profile remains offline and deterministic with `ExtractiveSummaryProvider`.

### Why pgvector is part of the MVP (not a bonus)

The assignment requires semantic retrieval. pgvector enables native vector storage/indexing in PostgreSQL and allows semantic scoring to participate directly in ranking.

### Why the tie-break contract is explicit and enforced in SQL

Stable ordering is required for predictable pagination and reproducible results. Explicit tie-break rules prevent ordering drift when scores are close.

### Why JDK 21 and not a newer JDK

JDK 21 is the verified baseline for this codebase and test toolchain. Newer JDKs (22+) currently break Byte Buddy/Mockito instrumentation used by tests.

## Running with Ollama

```bash
docker compose -f docker-compose.yml -f docker-compose.llm.yml up -d
docker compose exec ollama ollama pull llama3.2:1b
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run -Dspring-boot.run.profiles=ollama
```

Default profile behavior is unchanged: it stays offline and uses `ExtractiveSummaryProvider`.

## Testing

- Default lane:

```bash
./mvnw -q clean test
```

  - Runs unit tests and `@WebMvcTest` slices.
  - Does not require a running database.

- Integration lane:

```bash
docker compose up -d
./mvnw -q clean test -Dspring.profiles.active=integration
```

  - Runs integration coverage against PostgreSQL/pgvector (profile-gated).
  - Profile gating keeps the default lane fast and DB-free while still validating SQL/vector behavior when explicitly requested.

## Project layout

```text
src/main/java/com/neviswealth/searchapi
├── api
│   ├── dto
│   └── exception
├── client
├── config
├── document
├── embedding
├── search
└── summary

src/test/java/com/neviswealth/searchapi
├── api
├── client
├── document
├── embedding
├── search
└── summary
```

## Limitations and future work

- Hashing embeddings are weak outside the curated synonym dictionary; a production deployment would compute sentence-transformer embeddings offline (Spark + sentence-transformers) and sync to Postgres.
- Integration tests require a running Postgres.
- Testcontainers integration is intentionally out of scope.
- No authentication, no rate limiting, no multi-tenancy — out of scope.
- Optional LLM-based document summary (for example, Ollama + Llama 3.2 1B) behind a feature flag is a possible extension and is not implemented.