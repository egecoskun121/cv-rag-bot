package com.ege.cvrag.vectorstore;

import com.ege.cvrag.model.CvChunk;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.StringJoiner;

/**
 * A hand-rolled vector store over PostgreSQL + the pgvector extension.
 *
 * This is the part Spring AI's PgVectorStore hides. Doing it by hand shows the
 * three things that make vector search work in Postgres:
 *   1. the {@code vector(N)} column type (from the pgvector extension),
 *   2. the {@code <=>} cosine-distance operator for "nearest neighbour" search,
 *   3. an HNSW index so that search stays fast as the table grows.
 */
@Repository
public class PgVectorStore {

    private static final Logger log = LoggerFactory.getLogger(PgVectorStore.class);

    private final JdbcTemplate jdbc;
    private final int dimensions;

    public PgVectorStore(JdbcTemplate jdbc,
                         @Value("${app.rag.embedding-dimensions}") int dimensions) {
        this.jdbc = jdbc;
        this.dimensions = dimensions;
    }

    /** Creates the extension, table and index if they don't exist yet. */
    @PostConstruct
    public void initSchema() {
        jdbc.execute("CREATE EXTENSION IF NOT EXISTS vector");
        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS cv_chunk (
                    id        BIGSERIAL PRIMARY KEY,
                    content   TEXT NOT NULL,
                    section   TEXT,
                    embedding vector(%d) NOT NULL
                )
                """.formatted(dimensions));
        // HNSW index with cosine ops — matches the <=> operator we query with.
        jdbc.execute("""
                CREATE INDEX IF NOT EXISTS cv_chunk_embedding_idx
                    ON cv_chunk USING hnsw (embedding vector_cosine_ops)
                """);
        log.info("pgvector schema ready (cv_chunk, vector({}), HNSW cosine index)", dimensions);
    }

    /** Removes all stored chunks. Returns the number deleted. */
    public int deleteAll() {
        return jdbc.update("DELETE FROM cv_chunk");
    }

    /** Stores one chunk together with its embedding. */
    public void save(String content, String section, float[] embedding) {
        // The embedding is passed as a pgvector literal string and cast with ?::vector.
        jdbc.update("INSERT INTO cv_chunk (content, section, embedding) VALUES (?, ?, ?::vector)",
                content, section, toVectorLiteral(embedding));
    }

    /**
     * Returns the {@code topK} chunks most similar to the query embedding.
     * {@code embedding <=> query} is cosine DISTANCE (0 = identical), so we order
     * ascending and report {@code 1 - distance} as a similarity score.
     */
    public List<CvChunk> search(float[] queryEmbedding, int topK) {
        String vector = toVectorLiteral(queryEmbedding);
        return jdbc.query("""
                        SELECT content,
                               section,
                               1 - (embedding <=> ?::vector) AS score
                        FROM cv_chunk
                        ORDER BY embedding <=> ?::vector
                        LIMIT ?
                        """,
                (rs, rowNum) -> new CvChunk(
                        rs.getString("content"),
                        rs.getString("section"),
                        rs.getDouble("score")),
                vector, vector, topK);
    }

    /** Formats a float[] as a pgvector literal, e.g. "[0.12,-0.98,...]". */
    private String toVectorLiteral(float[] v) {
        StringJoiner sj = new StringJoiner(",", "[", "]");
        for (float f : v) {
            sj.add(Float.toString(f));
        }
        return sj.toString();
    }
}
