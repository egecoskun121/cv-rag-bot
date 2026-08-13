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
