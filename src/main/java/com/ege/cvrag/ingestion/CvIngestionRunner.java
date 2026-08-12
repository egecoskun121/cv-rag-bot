package com.ege.cvrag.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads the CV/portfolio document into the pgvector store at startup so the RAG
 * pipeline has something to retrieve. This is the "indexing" phase of RAG:
 *   read document -> split into chunks -> embed each chunk -> store vectors.
 *
 * We chunk *by Markdown section* rather than by a fixed token window. A CV is
 * naturally organised into headed sections (each job, Skills, Education...), and
 * a plain token splitter can slice a heading away from its bullets — e.g. the
 * "### Backend Developer — ilaBank" heading landing in a different chunk than
 * its responsibilities, so a query like "ilaBank responsibilities" fails to
 * retrieve/associate them. Keeping each heading together with its body makes
 * every chunk a self-labelled, semantically coherent unit.
 */
@Component
public class CvIngestionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CvIngestionRunner.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final Resource docs;
    private final boolean reloadOnStartup;

    public CvIngestionRunner(VectorStore vectorStore,
                             JdbcTemplate jdbcTemplate,
                             @Value("${app.ingestion.docs-location}") Resource docs,
                             @Value("${app.ingestion.reload-on-startup:true}") boolean reloadOnStartup) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.docs = docs;
        this.reloadOnStartup = reloadOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (reloadOnStartup) {
            // Dev convenience: wipe previous vectors so re-runs don't duplicate content.
            int deleted = jdbcTemplate.update("DELETE FROM vector_store");
            log.info("Cleared {} existing vector rows before re-ingestion", deleted);
        } else if (hasExistingVectors()) {
            log.info("Vector store already populated; skipping ingestion");
            return;
        }

        // 1. READ the document.
        String text;
        try (InputStream in = docs.getInputStream()) {
            text = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }

        // 2. SPLIT by Markdown section (## / ###), keeping each heading with its body.
        List<Document> chunks = splitByMarkdownSection(text);

        // 3. EMBED + STORE (the Ollama embedding model + pgvector do this under the hood).
        vectorStore.add(chunks);

        log.info("Ingested {} section chunks from {} into pgvector", chunks.size(), docs.getFilename());
    }

    /**
     * Splits Markdown into one Document per level-2/level-3 heading section.
     * The leading preamble (title + contact block, before the first ## heading)
     * becomes its own chunk. Each chunk carries a "section" metadata field.
     */
    private List<Document> splitByMarkdownSection(String text) {
        List<Document> chunks = new ArrayList<>();
        String[] lines = text.split("\n", -1);

        StringBuilder buf = new StringBuilder();
        String currentHeading = "Overview";

        for (String line : lines) {
            boolean isSection = line.startsWith("## ") || line.startsWith("### ");
            if (isSection && buf.toString().isBlank() == false) {
                addChunk(chunks, currentHeading, buf.toString());
                buf.setLength(0);
            }
            if (isSection) {
                currentHeading = line.replaceFirst("^#+\\s*", "").trim();
            }
            buf.append(line).append('\n');
        }
        if (!buf.toString().isBlank()) {
            addChunk(chunks, currentHeading, buf.toString());
        }
        return chunks;
    }

    private void addChunk(List<Document> chunks, String heading, String body) {
        Document doc = new Document(body.strip());
        doc.getMetadata().put("source", "cv.md");
        doc.getMetadata().put("section", heading);
        chunks.add(doc);
    }

    private boolean hasExistingVectors() {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM vector_store", Integer.class);
        return count != null && count > 0;
    }
}
