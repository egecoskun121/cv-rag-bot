# CV RAG Bot

[![CI](https://github.com/egecoskun121/cv-rag-bot/actions/workflows/ci.yml/badge.svg)](https://github.com/egecoskun121/cv-rag-bot/actions/workflows/ci.yml)
[![CodeQL](https://github.com/egecoskun121/cv-rag-bot/actions/workflows/codeql.yml/badge.svg)](https://github.com/egecoskun121/cv-rag-bot/actions/workflows/codeql.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A retrieval-augmented generation (RAG) service that answers questions about a CV,
grounded in the document itself. Runs fully locally, no API keys — Ollama for
inference, Postgres/pgvector for similarity search. The RAG pipeline is hand-rolled
(no Spring AI): direct HTTP calls to Ollama and plain SQL against pgvector.

## How it works

```
question ──▶ ChatController ──▶ RagService
                                   ├─ embed the question         (Ollama /api/embeddings)
                                   ├─ nearest chunks from pgvector (ORDER BY embedding <=> ?)
                                   ├─ build a context prompt
                                   └─ generate the answer        (Ollama /api/chat)
```

At startup, each `DocumentSource` (CV, GitHub projects, ...) is chunked
section-by-section, embedded, and stored in pgvector — see [Data sources](#data-sources).

## Stack

| Layer         | Technology                                               |
|---------------|-----------------------------------------------------------|
| Runtime       | Java 21, Spring Boot 3.3 (web + JDBC)                      |
| Inference     | Ollama — `qwen2.5:7b` (chat), `nomic-embed-text` (embed)   |
| Vector store  | PostgreSQL + pgvector (`vector(768)`, HNSW, cosine)        |
| Messaging     | Kafka (optional — event-driven ingestion + Q&A observability) |

## Prerequisites

- Java 21, Maven, Docker
- [Ollama](https://ollama.com)

## Running

```bash
ollama pull qwen2.5:7b
ollama pull nomic-embed-text
docker compose up -d
./mvnw spring-boot:run
```

UI: http://localhost:8081. Health: `GET /health`. API:

```bash
curl -s http://localhost:8081/api/v1/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"How many years of experience does Ege have?"}'
```

## Configuration

Settings live in `src/main/resources/application.yml` under `app.*` (models,
`top-k`, embedding dimensions). Replace `src/main/resources/docs/cv.md` with your
own CV — re-indexed on each startup.

### Database: local Docker or AWS RDS

`DB_URL` / `DB_USERNAME` / `DB_PASSWORD` env vars override the datasource; default
is local Docker Postgres. Point at RDS:

```bash
export DB_URL="jdbc:postgresql://<rds-endpoint>:5432/ragdb?sslmode=require"
export DB_USERNAME=ragadmin
export DB_PASSWORD="***"
./mvnw spring-boot:run
```

`PgVectorStore` creates the extension/table/index itself on first connect (RDS
Postgres 15.2+ ships pgvector). Restrict the RDS security group to your IP.

## Data sources

Each source implements `DocumentSource`; the active `DocumentReindexer` (sync or
Kafka) injects them all as `List<DocumentSource>` — adding a source never changes
the orchestrator (Open/Closed).

- **`cv.md`** — from the local classpath (`CvDocumentSource`, default) or **AWS
  S3** (`S3DocumentSource`, `app.docs.source: s3` + `app.docs.s3.{bucket,key,region}`).
  Only one is wired in (`@ConditionalOnProperty`). Credentials via the AWS default
  provider chain — none stored in the app.
- **GitHub projects** — the user's own repos via the GitHub API, tech stack from
  real language breakdown. Configure under `app.github`; a failure is skipped, not
  fatal.

**Re-index without a restart:**

```bash
curl -X POST http://localhost:8081/api/v1/reindex
# {"totalChunks":34,"sources":[{"source":"CV (s3://…/cv.md)","chunks":14,"status":"indexed"}, …]}
```

### Ingestion mode: sync or Kafka

`app.ingestion.mode`: **`sync`** (default) indexes inline, no infra. **`kafka`**
publishes one event per source; a consumer indexes async with retry + dead-letter:

```
reindex()/startup ──▶ document-ingest-requests ──▶ IngestionEventConsumer
                                                       fetch → embed → pgvector
                                                            │ N failed retries
                                                            ▼
                                             document-ingest-requests.DLT
```

Run with `docker compose up -d kafka` and `--app.ingestion.mode=kafka`.

### Q&A observability

With `app.qa.events.enabled=true`, every `/ask` publishes a `QaEvent` (question,
sections, top score, latency) to `qa-events`; a consumer aggregates stats:

```bash
curl http://localhost:8081/api/v1/qa/stats
# {"totalQuestions":3,"weakRetrievals":0,"avgLatencyMs":8668.3,"topSections":{...}}
```

`weakRetrievals` = answers below `app.qa.weak-score-threshold` (needs tuning per
corpus). Off by default — no Kafka dependency when disabled.

## Agentic RAG (alternative endpoint)

`POST /api/v1/ask` runs a fixed pipeline. `POST /api/v1/agent/ask` runs an agent
that decides when/what to retrieve via a tool-use loop (`search_cv` tool):

```
question ──▶ model (with a search_cv tool)
                 ├─ tool call?  → run it, feed results back, loop
                 └─ answer?     → done
```

```bash
curl -s http://localhost:8081/api/v1/agent/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"What did Ege do at ilaBank?"}'
```

## Evaluation

A gold Q&A set (`src/test/resources/eval/cases.json`) scores Recall@k, MRR and
keyword coverage instead of eyeballing answers. Pure scoring is unit-tested in CI;
the full run needs a live stack:

```bash
mvn test -Deval=true -Dtest=RagEvaluationHarnessTest
```

Writes `target/eval-report.md`.

## Notes

- `top-k` is high because a one-page CV fits the whole context window;
  `nomic-embed-text` is English-centric, so a multilingual model (`bge-m3`) would
  let a lower `top-k` work reliably.
- `app.ingestion.reload-on-startup` wipes and re-embeds every boot; disable for a
  persistent store.

## License

[MIT](LICENSE)
