package com.ege.cvrag.ingestion;

import com.ege.cvrag.vectorstore.PgVectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Indexes the CV into the vector store at startup. Runs first ({@code @Order(1)}):
 * it clears the store on a reload, then any other source (e.g. GitHub projects)
 * appends to it afterwards.
 */
@Component
@Order(1)
public class CvIngestionRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CvIngestionRunner.class);

    private final MarkdownIndexer indexer;
    private final PgVectorStore vectorStore;
    private final Resource docs;
    private final boolean reloadOnStartup;

    public CvIngestionRunner(MarkdownIndexer indexer,
                             PgVectorStore vectorStore,
                             @Value("${app.ingestion.docs-location}") Resource docs,
                             @Value("${app.ingestion.reload-on-startup:true}") boolean reloadOnStartup) {
        this.indexer = indexer;
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

        String markdown;
        try (InputStream in = docs.getInputStream()) {
            markdown = StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        }

        int count = indexer.index(markdown);
        log.info("Ingested {} CV section chunks from {} into pgvector", count, docs.getFilename());
    }
}
