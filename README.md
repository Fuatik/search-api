# Nevis Search API

Nevis Search API is a Java/Spring Boot home assignment that provides client/document management and ranked semantic-lexical search across clients and documents. It combines deterministic embeddings, PostgreSQL full-text search, trigram similarity, and pgvector cosine distance in a reproducible offline pipeline. The project is designed for local execution with Dockerized Postgres and Maven Wrapper.

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

`GET /api/v1/search` returns both clients and documents in a unified ranked list. Each item has a `type` field: `CLIENT` or `DOCUMENT`.

To try document search (semantic):

1. `POST /api/v1/clients`
2. `POST /api/v1/clients/{id}/documents` with title `Electric Utility Bill - May` and content containing `address proof`
3. `GET /api/v1/search?q=address+proof` — the utility bill ranks first.

To try client search:

1. `POST /api/v1/clients` with `email = john.doe@neviswealth.com`
2. `GET /api/v1/search?q=NevisWealth` — the client ranks first (matched by trigram on the email).

## API

| Method | Path | Description | Status codes |
|---|---|---|---|
| POST | `/api/v1/clients` | creates a client | `201`, `400`, `409`, `422`, `500` |
| GET | `/api/v1/clients/{id}` | returns a client by id | `200`, `400`, `404`, `500` |
| POST | `/api/v1/clients/{id}/documents` | creates a document for a client | `201`, `400`, `404`, `422`, `500` |
| GET | `/api/v1/documents/{id}` | returns a document by id | `200`, `400`, `404`, `500` |
| GET | `/api/v1/search?q=&page=&size=` | returns mixed `CLIENT` and `DOCUMENT` items | `200`, `400`, `500` |

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

## Example: document search

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
      "type": "DOCUMENT",
      "id": "be55ab57-9f05-4f22-8499-3f48253f42a0",
      "documentId": "be55ab57-9f05-4f22-8499-3f48253f42a0",
      "clientId": "0a1c1733-f0b1-4b0e-ad43-adb60564192c",
      "title": "Electric Utility Bill - May",
      "summary": "Monthly utility bill for 340 kWh. Reference: address proof. Customer: John Doe.",
      "scores": {
        "lexical": 0.6000000238418579,
        "trigram": 0.18666666746139526,
        "semantic": 0.31622779216418806,
        "finalScore": 0.43220168906736767
      },
      "matchReasons": ["FTS_MATCH", "TRIGRAM_MATCH"],
      "createdAt": "2026-10-09T00:11:42.190626Z"
    },
    {
      "type": "DOCUMENT",
      "id": "1529aac8-1d72-410f-95e2-251b4e423c56",
      "documentId": "1529aac8-1d72-410f-95e2-251b4e423c56",
      "clientId": "0a1c1733-f0b1-4b0e-ad43-adb60564192c",
      "title": "Bank Statement - June",
      "summary": "Bank statement for June. Monthly account summary. Address on file confirmed.",
      "scores": {
        "lexical": 0.5,
        "trigram": 0.10526315867900848,
        "semantic": 0.15811389136993237,
        "finalScore": 0.3184868017767039
      },
      "matchReasons": ["FTS_MATCH", "TRIGRAM_MATCH"],
      "createdAt": "2026-10-09T00:11:48.478183Z"
    }
  ]
}
```

The third seeded document (Passport) does not appear in the results because it has no relevance signals for 'address proof': no synonym match, no FTS match, low trigram similarity, and no semantic overlap. This demonstrates that the ranking filter does not return false positives.

## Example: client search

`GET /api/v1/search` also matches clients by `first_name`, `last_name`, `email`, and `description`. Client search uses FTS + trigram only (no embeddings), because these fields are short and keyword-driven.

```bash
curl "http://localhost:8080/api/v1/search?q=NevisWealth"
```

```json
{
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1,
  "items": [
    {
      "type": "CLIENT",
      "id": "0a1c1733-f0b1-4b0e-ad43-adb60564192c",
      "documentId": null,
      "clientId": null,
      "title": "John Doe",
      "summary": "High net worth client",
      "scores": { "lexical": 0.0, "trigram": 0.47999998927116394, "semantic": 0.0, "finalScore": 0.14399999678134917 },
      "matchReasons": ["TRIGRAM_MATCH"],
      "createdAt": "2026-10-09T00:11:23.934605Z"
    }
  ]
}
```

Client candidates and document candidates are merged into a single ranked list, sorted by `finalScore DESC` with the same tie-break rules.

## Architecture

- API layer (`com.neviswealth.searchapi.api`): REST controllers and RFC7807 error mapping.
- Service layer: client/document/search orchestration.
- Search pipeline:
  - `QueryNormalizer`
  - `SynonymExpander` (YAML-driven)
  - Document candidates: native SQL with `ts_rank_cd`, `pg_trgm` similarity, `pgvector` cosine (`<=>`)
  - Client candidates: native SQL with `ts_rank_cd`, `pg_trgm` similarity (no embeddings)
  - Java weighted merge — documents: `lexical=0.5`, `semantic=0.3`, `trigram=0.2`; clients: `lexical=0.7`, `trigram=0.3`
  - unified ranked list with `type: CLIENT | DOCUMENT`
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
- Tie-break order: `finalScore DESC` → `lexicalScore DESC` → `createdAt DESC` → `id ASC`.

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

### Why client search uses FTS + trigram only (no embeddings)

Client names, emails, and short descriptions are keyword-driven — embeddings add little value for these fields. FTS catches exact token matches; trigram handles partial matches and typos (e.g. `NevisWealth` against `john.doe@neviswealth.com`). Document search keeps embeddings because document content is longer and benefits from semantic similarity.

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
  - `ClientSearchRepositoryIT`
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
- Client search uses FTS + trigram only (no embeddings), which is sufficient for short keyword-driven fields but would not catch loose semantic queries such as "person who pays utility bills".
- Integration tests require a running Postgres.
- Testcontainers integration is intentionally out of scope.
- No authentication, no rate limiting, no multi-tenancy — out of scope.
- Ollama-backed LLM summary is implemented and works locally; running it on Railway's shared CPU is impractical (see "Note on Ollama deployment").