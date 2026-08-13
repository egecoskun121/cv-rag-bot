package com.ege.cvrag.ingestion;

import com.ege.cvrag.constant.RagBotConstants;
import com.ege.cvrag.llm.OllamaClient;
import com.ege.cvrag.model.ingestion.MarkdownSection;
import com.ege.cvrag.vectorstore.PgVectorStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Indexes Markdown into the vector store, reusable across sources (CV, GitHub…).
 *
 * We chunk *by Markdown section* rather than by a fixed token window: each ## / ###
 * heading is kept together with its body, so every chunk is a self-labelled,
 * semantically coherent unit (a heading is never sliced away from its content).
 */
@Component
public class MarkdownIndexer {

    private final OllamaClient ollama;
    private final PgVectorStore vectorStore;

    public MarkdownIndexer(OllamaClient ollama, PgVectorStore vectorStore) {
        this.ollama = ollama;
        this.vectorStore = vectorStore;
    }

    /** Splits the Markdown into sections, embeds each, and stores it. Returns the section count. */
    public int index(String markdown) {
        List<MarkdownSection> sections = splitByMarkdownSection(markdown);
        sections.forEach(section ->
                vectorStore.save(section.body(), section.heading(), ollama.embed(section.body())));
        return sections.size();
    }

    /**
     * Splits Markdown into one section per level-2/level-3 heading, keeping the
     * heading with its body. Any leading preamble (before the first heading)
     * becomes its own section.
     */
    private List<MarkdownSection> splitByMarkdownSection(String markdown) {
        List<MarkdownSection> sections = new ArrayList<>();
        String[] lines = markdown.split("\n", -1);

        StringBuilder buf = new StringBuilder();
        String currentHeading = RagBotConstants.DEFAULT_SECTION_HEADING;

        for (String line : lines) {
            boolean isSection = line.startsWith(RagBotConstants.MARKDOWN_H2_PREFIX)
                    || line.startsWith(RagBotConstants.MARKDOWN_H3_PREFIX);
            if (isSection && !buf.toString().isBlank()) {
                sections.add(new MarkdownSection(currentHeading, buf.toString().strip()));
                buf.setLength(0);
            }
            if (isSection) {
                currentHeading = line.replaceFirst("^#+\\s*", "").trim();
            }
            buf.append(line).append('\n');
        }
        if (!buf.toString().isBlank()) {
            sections.add(new MarkdownSection(currentHeading, buf.toString().strip()));
        }
        return sections;
    }
}
