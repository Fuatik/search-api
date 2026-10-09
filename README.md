# Nevis Search API

Nevis Search API is a Java/Spring Boot home assignment that provides client/document management and ranked semantic-lexical document search. It combines deterministic embeddings, PostgreSQL full-text search, trigram similarity, and pgvector cosine distance in a reproducible offline pipeline. The project is designed for local execution with Dockerized Postgres and Maven Wrapper.

## Requirements

- JDK 21 (LTS) — the verified baseline for this project. The project targets Java 21 bytecode.
- Docker — for PostgreSQL 16 with pgvector.
- Maven Wrapper (`./mvnw`) — no global Maven required.

## Quick start

Option 1 — Full stack in Docker:

```bash
docker compose up --build
```

Then open `http://localhost:8080/swagger-ui.html`

Option 2 — Only Postgres in Docker, app via Maven (development):

```bash
docker compose up -d postgres
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run
```

Health check (application responding):

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

Swagger UI:

- `http://localhost:8080/swagger-ui.html`

## Live deployment

- API: `https://search-api-production-8bfe.up.railway.app`
- Swagger UI: `https://search-api-production-8bfe.up.railway.app/swagger-ui.html`
- Health: `https://search-api-production-8bfe.up.railway.app/actuator/health`

Deployed with the `default` profile (`ExtractiveSummaryProvider`). PostgreSQL 16 + pgvector is managed by Railway.

To try semantic search:

1. `POST /api/v1/clients`
2. `POST /api/v1/clients/{id}/documents` with title `Electric Utility Bill - May` and content containing `address proof`
3. `GET /api/v1/search?q=address+proof` — the utility bill ranks first.

## API

| Method | Path | Status codes |
|---|---|---|
| POST | `/api/v1/clients` | `201`, `400`, `409`, `422`, `500` |
| GET | `/api/v1/clients/{id}` | `200`, `400`, `404`, `500` |
| POST | `/api/v1/clients/{id}/documents` | `201`, `400`, `404`, `422`, `500` |
| GET | `/api/v1/documents/{id}` | `200`, `400`, `404`, `500` |
| GET | `/api/v1/search?q=&page=&size=` | `200`, `400`, `500` |

500 is a fallback for unexpected errors and returns a generic RFC7807 body.

Error responses use RFC7807 `ProblemDetail`.

## Error responses

Example `400 Bad Request` for invalid email:

```bash
curl -i -X POST "http://localhost:8080/api/v1/clients" \
  -H "Content-Type: application/json" \
  -d '{
    "first_name": "John",
    "last_name": "Doe",
    "email": "not-an-email",
    "description": "Example client",
    "social_links": ["https://example.com/john"]
  }'
```

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "email must be a well-formed email address",
  "instance": "/api/v1/clients",
  "errors": {
    "email": "must be a well-formed email address"
  }
}
```

## Example: semantic search

This walkthrough shows how `address proof` returns `Electric Utility Bill - May` at the top. Ranking is produced by synonym expansion + FTS + trigram + pgvector cosine, then merged with weighted scoring.

Create a client:

```bash
curl -X POST "http://localhost:8080/api/v1/clients" \
  -H "Content-Type: application/json" \
  -d '{
    "first_name": "John",
    "last_name": "Doe",
    "email": "john.doe@example.com",
    "description": "Seeded search demo client",
    "social_links": []
  }'
```

Add document 1:

```bash
curl -X POST "http://localhost:8080/api/v1/clients/e0b3bf9a-feea-4a8f-8693-444b63928fcf/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Electric Utility Bill - May",
    "content": "Monthly utility bill for 340 kWh. Reference: address proof. Customer: John Doe."
  }'
```

Add document 2:

```bash
curl -X POST "http://localhost:8080/api/v1/clients/e0b3bf9a-feea-4a8f-8693-444b63928fcf/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Bank Statement - June",
    "content": "Bank statement for June. Monthly account summary. Address on file confirmed."
  }'
```

Add document 3:

```bash
curl -X POST "http://localhost:8080/api/v1/clients/e0b3bf9a-feea-4a8f-8693-444b63928fcf/documents" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Passport",
    "content": "Identity document. Issued by government. Photo and signature."
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
      "documentId": "351a514d-7edc-48c9-88f9-a69f4e26938d",
      "clientId": "e0b3bf9a-feea-4a8f-8693-444b63928fcf",
      "title": "Electric Utility Bill - May",
      "summary": "Monthly utility bill for 340 kWh. Reference: address proof. Customer: John Doe.",
      "scores": {
        "lexical": 0.6000000238418579,
        "trigram": 0.18666666746139526,
        "semantic": 0.31622778273986474,
        "finalScore": 0.43220168624007055
      },
      "matchReasons": ["FTS_MATCH", "TRIGRAM_MATCH"],
      "createdAt": "2026-10-08T22:32:16.244099Z"
    },
    {
      "documentId": "c338e448-ee1d-4ae4-91c4-ce8a53baedd3",
      "clientId": "e0b3bf9a-feea-4a8f-8693-444b63928fcf",
      "title": "Bank Statement - June",
      "summary": "Bank statement for June. Monthly account summary. Address on file confirmed.",
      "scores": {
        "lexical": 0.5,
        "trigram": 0.10526315867900848,
        "semantic": 0.15811389136993237,
        "finalScore": 0.3184868017767039
      },
      "matchReasons": ["FTS_MATCH", "TRIGRAM_MATCH"],
      "createdAt": "2026-10-08T22:32:21.418951Z"
    }
  ]
}
```

The third seeded document (Passport) does not appear in the results because it has no relevance signals for 'address proof': no synonym match, no FTS match, low trigram similarity, and no semantic overlap. This demonstrates that the ranking filter does not return false positives.

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

### Why in-memory pagination instead of SQL OFFSET

The native query combines FTS, trigram, and pgvector cosine into a weighted
score computed in Java. Pagination is applied after ranking with a hard
candidate cap of 200. Pushing OFFSET into SQL would require materializing
the ranked set anyway, so the split is deliberate.

### Why pgvector is part of the MVP (not a bonus)

The assignment requires semantic retrieval. pgvector enables native vector storage/indexing in PostgreSQL and allows semantic scoring to participate directly in ranking.

### Why the tie-break contract is explicit and enforced in SQL

Stable ordering is required for predictable pagination and reproducible results. Explicit tie-break rules prevent ordering drift when scores are close.

### Why target JDK 21

JDK 21 is an LTS release and the current supported baseline for Spring Boot 3.4.x.
Non-LTS JDKs may lag in tooling support across transitive test dependencies
such as Byte Buddy and Mockito. Targeting an LTS keeps the project reproducible
and aligned with production deployments of Spring Boot 3.4.x.

## Running with Ollama

```bash
docker compose -f docker-compose.yml -f docker-compose.llm.yml up -d
docker compose exec ollama ollama pull llama3.2:1b
JAVA_HOME=$(/usr/libexec/java_home -v 21) ./mvnw spring-boot:run -Dspring-boot.run.profiles=ollama
```

The default profile stays offline and uses `ExtractiveSummaryProvider`; `OllamaSummaryProvider` is opt-in via the `ollama` profile or `app.summary.provider=ollama`.

Input:
"This employment agreement is made between Acme Corporation and Jane Smith,
effective January 1, 2026. The employee agrees to work 40 hours per week as
a Senior Engineer. Compensation is set at 150,000 USD annually. The contract
may be terminated with 30 days notice. Confidentiality and non-compete clauses
apply for a period of 12 months after termination."

Extractive (default):
"This employment agreement is made between Acme Corporation and Jane Smith,
effective January 1, 2026. The employee agrees to work 40 hours per week as
a Senior Engineer. Compensation is set at 150,000 USD annually."

Ollama (Llama 3.2 1B, verified locally):
"The employment agreement between Acme Corporation and Jane Smith includes a
12-month non-compete clause and 30-day notice period for termination. Jane will
work 40 hours per week as a Senior Engineer and receive an annual salary of
$150,000. The agreement also includes confidentiality and non-compete clauses."

Extractive preserves the original sentences verbatim; Ollama paraphrases and reorders content. The default extractive provider is recommended for offline and CI usage; the Ollama path demonstrates how the pluggable SummaryProvider interface integrates an LLM in production.

### Note on Ollama deployment

The `OllamaSummaryProvider` is fully implemented and unit-tested with a mocked `ChatModel`, and verified end-to-end locally with `llama3.2:1b`: the same `Employment Contract` input produces a paraphrased LLM summary, distinct from the extractive output.

A Railway deployment of Ollama was attempted but abandoned. Railway's shared CPU does not provide sufficient compute for LLM inference: `llama.cpp` detects the host's 96 cores and spawns 48 threads, exceeding the container's CPU quota, which causes request timeouts. Ollama 1.x also no longer honors `OLLAMA_NUM_THREADS` (legacy variable, not propagated to `llama-server`). The live deployment therefore uses the default `ExtractiveSummaryProvider` for stability and reproducibility.

The Ollama path remains fully functional locally; see the commands above.

## Testing

```bash
./mvnw -q clean test                                     # default lane, no DB
./mvnw -q clean test -Dspring.profiles.active=integration  # integration lane, needs Postgres
```

- Default lane classes:
  - `ClientControllerTest`, `DocumentControllerTest`, `SearchControllerTest`
  - `ClientServiceTest`, `DocumentServiceTest`, `SearchServiceTest`
  - `QueryNormalizerTest`, `SynonymExpanderTest`
  - `HashingEmbeddingProviderTest`
  - `ExtractiveSummaryProviderTest`, `OllamaSummaryProviderTest`
- Integration lane classes (with `integration` profile):
  - `RepositorySmokeTest`
  - `DocumentSearchRepositorySearchIT`
  - `SearchRankingAcceptanceIT`

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
- Ollama-backed LLM summary is implemented and works locally; running it on Railway's shared CPU is impractical (see "Note on Ollama deployment").