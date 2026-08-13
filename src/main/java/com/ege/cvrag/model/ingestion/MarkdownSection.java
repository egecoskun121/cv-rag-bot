package com.ege.cvrag.model.ingestion;

/** A Markdown section: its heading and full body text (heading included in body). */
public record MarkdownSection(String heading, String body) {}
