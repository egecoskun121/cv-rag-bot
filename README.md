# CV RAG Bot

[![CI](https://github.com/egecoskun121/cv-rag-bot/actions/workflows/ci.yml/badge.svg)](https://github.com/egecoskun121/cv-rag-bot/actions/workflows/ci.yml)
[![CodeQL](https://github.com/egecoskun121/cv-rag-bot/actions/workflows/codeql.yml/badge.svg)](https://github.com/egecoskun121/cv-rag-bot/actions/workflows/codeql.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot 3.3](https://img.shields.io/badge/Spring%20Boot-3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A small retrieval-augmented generation (RAG) service that answers questions about
a CV, grounded in the document itself. It runs fully locally — no API keys — using
Ollama for inference and Postgres/pgvector for similarity search.

The RAG pipeline is written by hand (no Spring AI): direct HTTP calls to Ollama and
plain SQL against pgvector, so the retrieval mechanics are visible rather than hidden
behind a framework.

## How it works

```
question ──▶ ChatController ──▶ RagService
                                   ├─ embed the question         (Ollama /api/embeddings)
                                   ├─ nearest chunks from pgvector (ORDER BY embedding <=> ?)
                                   ├─ build a context prompt
                                   └─ generate the answer        (Ollama /api/chat)
```

At startup, `CvIngestionRunner` reads `cv.md`, splits it by Markdown section (keeping
each heading with its body), embeds each section, and stores the vectors in pgvector.

## Stack

| Layer            | Technology                                             |
|------------------|--------------------------------------------------------|
| Runtime          | Java 21, Spring Boot 3.3 (web + JDBC)                   |
| Inference        | Ollama — `qwen2.5:7b` (chat), `nomic-embed-text` (embed)|
| Vector store     | PostgreSQL + pgvector (`vector(768)`, HNSW, cosine)     |
| HTTP / DB access  | `RestClient`, `JdbcTemplate`                           |

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

Open http://localhost:8081 for the chat UI, or call the API:

```bash
curl -s http://localhost:8081/api/v1/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"How many years of experience does Ege have?"}'
```

Health check: `GET http://localhost:8081/health`.

## Configuration

Key settings live in `src/main/resources/application.yml` under `app.*`:
chat/embedding models, `top-k`, and embedding dimensions. Replace
`src/main/resources/docs/cv.md` with your own CV; it is re-indexed on each startup.

### Database: local Docker or AWS RDS

`spring.datasource.{url,username,password}` read from `DB_URL` / `DB_USERNAME` /
`DB_PASSWORD` env vars, defaulting to the local Docker Postgres
(`docker-compose up -d postgres`). Point at an RDS instance instead without
touching the file:

```bash
export DB_URL="jdbc:postgresql://<rds-endpoint>:5432/ragdb?sslmode=require"
export DB_USERNAME=ragadmin
export DB_PASSWORD="***"
./mvnw spring-boot:run
```

`PgVectorStore` creates the `vector` extension, table and HNSW index itself
(`@PostConstruct`) — RDS Postgres 15.2+ ships pgvector, so no manual schema setup is
needed beyond `CREATE EXTENSION vector` (already covered by that init step; if your
IAM role can't `CREATE EXTENSION`, run it once as the master user). Restrict the
instance's security group to your IP; don't make it publicly open.

## Data sources

Ingestion is source-pluggable. Each source implements `DocumentSource`
(`markdown()`), and the active `DocumentReindexer` (sync or Kafka — see below) gets
them all injected as a `List<DocumentSource>` — so **adding a source never changes
the orchestrator**,
you just add a `@Component` (Open/Closed). It runs at startup via a custom
`@RunOnStartup` annotation (see `startup/`), and everything is chunked
section-by-section by the same `MarkdownIndexer`.

- **`cv.md`** — the CV. Loaded from the local classpath by `CvDocumentSource`
  (default), or from **AWS S3** by `S3DocumentSource` — switch with
  `app.docs.source: s3` and set `app.docs.s3.{bucket,key,region}`. Exactly one is
  wired in (`@ConditionalOnProperty`), so the orchestrator is unaware of the
  origin. Credentials come from the AWS default provider chain (env vars, profile,
  or an instance/container role) — none are stored in the app.
- **GitHub projects** — the user's own (non-fork, described) repositories, fetched
  live from the GitHub API with their real language breakdown as the tech stack
  (`GitHubProjectsSource`). Configure under `app.github` (`enabled`, `user`,
  optional `token`); a GitHub failure is logged and skipped — the CV bot still works.

So the bot answers about the CV *and* the projects ("what tech stack does
cv-rag-bot use?", "what projects has Ege built?").

**Re-index without a restart:** `POST /api/v1/reindex` re-runs the exact same
ingestion that fires at startup and returns the per-source chunk breakdown. Handy
with the S3 source — update `cv.md` in the bucket, then:

```bash
curl -X POST http://localhost:8081/api/v1/reindex
# {"totalChunks":34,"sources":[{"source":"CV (s3://…/cv.md)","chunks":14,"status":"indexed"}, …]}
```

### Ingestion mode: sync or Kafka

`app.ingestion.mode` picks how sources are indexed, behind the `DocumentReindexer`
interface (the callers — startup hook and `/reindex` — don't change):

- **`sync`** (default) — `SyncDocumentReindexer` indexes each source inline, on the
  calling thread. Zero infrastructure; a failing source is logged and skipped.
- **`kafka`** — `KafkaDocumentReindexer` publishes one event per source and returns
  immediately; a consumer indexes them asynchronously, with retry and a
  dead-letter topic:

```
reindex()/startup ──▶ topic: document-ingest-requests ──▶ IngestionEventConsumer
   (publish per source)                                     fetch markdown → embed → pgvector
                                                                    │ fails N retries
                                                                    ▼
                                                   document-ingest-requests.DLT (parked)
```

The event carries only the source *name*, so the consumer re-fetches fresh content
— a transient failure (e.g. a GitHub rate limit) is retried against live data and,
if it keeps failing, parked in the DLT while the other sources still index. Run it
with Kafka up (`docker compose up -d kafka`) and `--app.ingestion.mode=kafka`.

### Q&A observability (event stream)

Where the Kafka ingestion pipeline is the *write* side, this is the *read* side made
observable. With `app.qa.events.enabled=true`, every `/ask` publishes a `QaEvent`
(question, retrieved sections, best similarity score, answer, latency) to a
`qa-events` topic — off the request path, so answering is unaffected. A consumer
folds events into in-memory stats, exposed at `GET /api/v1/qa/stats`:

```bash
curl http://localhost:8081/api/v1/qa/stats
# {"totalQuestions":3,"weakRetrievals":0,"avgLatencyMs":8668.3,
#  "topSections":{"Languages":3,"Education":3,"Certificates":2, …}}
```

`weakRetrievals` counts answers whose best score fell below
`app.qa.weak-score-threshold` — a proxy for "probably couldn't ground this". Note
the threshold needs tuning per corpus: with a one-page CV and a generous top-K, even
an off-topic question finds a nearest neighbour above 0.5, so the default rarely
fires. The publisher is optional (`ObjectProvider`), so with the feature off the RAG
service emits nothing.

## Agentic RAG (alternative endpoint)

`POST /api/v1/ask` runs a **fixed** pipeline (always embed → search → answer).
`POST /api/v1/agent/ask` instead runs an **agent** — the model decides *when* and
*with what query* to retrieve, via a tool-use loop:

```
question ──▶ model (with a `search_cv` tool)
                 │
                 ├─ replies with a tool call? → run search_cv, feed results back, loop
                 └─ replies with an answer?   → done
```

Components: `agent/CvSearchTool` (the retrieval tool the model can call) and
`agent/AgentService` (the loop / "agent harness", capped by `app.agent.max-iterations`).
This uses Ollama's tool-calling — the model emits structured `tool_calls`, the
harness executes them and returns the results as `tool` messages.

```bash
curl -s http://localhost:8081/api/v1/agent/ask \
  -H 'Content-Type: application/json' \
  -d '{"question":"What did Ege do at ilaBank?"}'
```

## Evaluation

A small evaluation harness measures RAG quality against a gold Q&A set
(`src/test/resources/eval/cases.json`) instead of eyeballing answers:

- **Retrieval:** Recall@k and MRR — is the expected CV section retrieved, and how high?
- **Answer:** keyword coverage — does the answer contain the expected facts?

The pure scoring (`RetrievalMetrics`) is unit-tested in CI. The full run needs a
live Ollama + pgvector, so it is off by default and runs as a local tool:

```bash
mvn test -Deval=true -Dtest=RagEvaluationHarnessTest
```

It writes `target/eval-report.md`. This is how you can quantify improvements —
e.g. show that switching to a multilingual embedding model raises Recall@k.

## Notes

- `top-k` is set high because a one-page CV fits entirely in the context window.
  `nomic-embed-text` is English-centric, so a Turkish query can rank the relevant
  chunk low; a multilingual embedding model (e.g. `bge-m3`) would make selective,
  low `top-k` retrieval reliable.
- `app.ingestion.reload-on-startup` wipes and re-embeds on every boot; disable it
  for a persistent store.

## License

Released under the [MIT License](LICENSE).
