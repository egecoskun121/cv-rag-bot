package com.ege.cvrag.ingestion;

import com.ege.cvrag.llm.OllamaClient;
import com.ege.cvrag.vectorstore.PgVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Indexes the CV into the vector store at startup — the "indexing" half of RAG:
 *   read cv.md -> split into sections -> embed each section -> store the vector.
 *
 * We chunk *by Markdown section* rather than by a fixed token window. A CV is
 * organised into headed sections (each job, Skills, Education...), and a plain
 * token splitter can slice a heading away from its bullets — e.g. the
 * "### Backend Developer — ilaBank" heading landing in a different chunk than
 * its responsibilities, which then fails to retrieve for "ilaBank
 * responsibilities". Keeping each heading with its body makes every chunk a
 * self-labelled, semantically coherent unit.
 */
@Component
public class CvIngestionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CvIngestionRunner.class);

    private final OllamaClient ollama;
    private final PgVectorStore vectorStore;
    private final Resource docs;
    private final boolean reloadOnStartup;

    public CvIngestionRunner(OllamaClient ollama,
                             PgVectorStore vectorStore,
                             @Value("${app.ingestion.docs-location}") Resource docs,
                             @Value("${app.ingestion.reload-on-startup:true}") boolean reloadOnStartup) {
        this.ollama = ollama;
        this.vectorStore = vectorStore;
        this.docs = docs;
        this.reloadOnStartup = reloadOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (reloadOnStartup) {
            int deleted = vectorStore.deleteAll();
            log.info("Cleared {} existing vector rows before re-ingestion", deleted);
        }

        // 1. READ the document.
        String text;
        try (InputStream in = docs.getInputStream()) {
            text = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }

        // 2. SPLIT by Markdown section (## / ###), keeping each heading with its body.
        List<Section> sections = splitByMarkdownSection(text);

        // 3. EMBED each section and STORE it with its vector.
        for (Section s : sections) {
            float[] embedding = ollama.embed(s.body());
            vectorStore.save(s.body(), s.heading(), embedding);
        }

        log.info("Ingested {} section chunks from {} into pgvector",
                sections.size(), docs.getFilename());
    }

    /**
     * Splits Markdown into one section per level-2/level-3 heading, keeping the
     * heading with its body. The leading preamble (title + contact block, before
     * the first ## heading) becomes its own section.
     */
    private List<Section> splitByMarkdownSection(String text) {
        List<Section> sections = new ArrayList<>();
        String[] lines = text.split("\n", -1);

        StringBuilder buf = new StringBuilder();
        String currentHeading = "Overview";

        for (String line : lines) {
            boolean isSection = line.startsWith("## ") || line.startsWith("### ");
            if (isSection && !buf.toString().isBlank()) {
                sections.add(new Section(currentHeading, buf.toString().strip()));
                buf.setLength(0);
            }
            if (isSection) {
                currentHeading = line.replaceFirst("^#+\\s*", "").trim();
            }
            buf.append(line).append('\n');
        }
        if (!buf.toString().isBlank()) {
            sections.add(new Section(currentHeading, buf.toString().strip()));
        }
        return sections;
    }

    private record Section(String heading, String body) {}
}
