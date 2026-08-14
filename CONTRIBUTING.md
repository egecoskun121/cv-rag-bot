# Contributing

## Adding a new `DocumentSource`

Every external content source (GitHub, Medium, ...) follows the same shape. To add
one:

1. **Model DTOs** — `model/<domain>/`. Prefer records; use plain classes + Lombok
   `@Getter @Setter` only when the mapping needs mutable fields (e.g. XML binding).
2. **Client** — `<domain>/<Domain>Api.java`, a `@HttpExchange` interface. Build it in
   `config/<Domain>ClientConfig.java` (`RestClient` + `HttpServiceProxyFactory`).
3. **Formatter** — `<domain>/<Domain>Formatter.java`, a `@Component` implementing
   `markdown.DocumentFormatter<T>` (`T` = the domain type, or a small record if the
   input has more than one part, e.g. `GitHubProjectView`). Build the Markdown with
   `markdown.MarkdownSectionBuilder` (`heading().body().field()...build()`) so
   every source renders consistently.
4. **Source** — `<domain>/<Domain>DocumentSource.java`, implements `DocumentSource`,
   with the formatter injected (not called statically):
   ```java
   @Component
   @Order(N)   // next free slot
   @ConditionalOnProperty(prefix = "app.<domain>", name = "enabled", havingValue = "true")
   ```
   A missing/empty response should index 0 chunks, never throw — a source with no
   content yet (or a transient failure) shouldn't break ingestion.
5. **Config** — `app.<domain>.{enabled, ...}` in `application.yml`, off by default
   unless it's local/free (mirror the GitHub/Medium blocks).
6. **Constants** — magic strings go in `constant/RagBotConstants`.
7. **Tests**:
   - Formatter: pure unit test (no I/O).
   - Client/parser (if non-trivial mapping, e.g. XML): unit test with a fixed sample.
   - Source: integration test against an in-process `HttpServer` stub — no real
     network, no account needed.
8. **README** — one short bullet under "Data sources".
9. Verify locally end-to-end (`./mvnw spring-boot:run`, check the startup ingest log).

Existing examples to copy from: `github/` (REST/JSON) and `medium/` (RSS/XML).

## Conventions

- `Objects.isNull(x)` / `Objects.nonNull(x)`, not `== null` / `!= null`.
- No magic strings — `RagBotConstants`.
- Streams over manual `for` loops for collection processing.
- Small, focused PRs; commit messages and README stay short.
