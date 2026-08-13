package com.ege.cvrag.ingestion;

import com.ege.cvrag.constant.RagBotConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * The CV document source that reads {@code cv.md} from the local classpath.
 * Active when {@code app.docs.source=classpath} (the default) — the S3 variant
 * ({@link S3DocumentSource}) takes over when set to {@code s3}. Indexed first
 * ({@code @Order(1)}).
 */
@Component
@Order(1)
@ConditionalOnProperty(prefix = "app.docs", name = "source",
        havingValue = RagBotConstants.DOCS_SOURCE_CLASSPATH, matchIfMissing = true)
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
