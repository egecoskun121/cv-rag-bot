package com.ege.cvrag.ingestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/** The CV document source (reads {@code cv.md}). Indexed first ({@code @Order(1)}). */
@Component
@Order(1)
public class CvDocumentSource implements DocumentSource {

    private final Resource docs;

    public CvDocumentSource(@Value("${app.ingestion.docs-location}") Resource docs) {
        this.docs = docs;
    }

    @Override
    public String name() {
        return "CV (" + docs.getFilename() + ")";
    }

    @Override
    public String markdown() {
        try (InputStream in = docs.getInputStream()) {
            return StreamUtils.copyToString(in, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read CV document", e);
        }
    }
}
