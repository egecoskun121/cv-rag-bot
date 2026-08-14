package com.ege.cvrag.medium;

import com.ege.cvrag.ingestion.DocumentSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Medium posts as a {@link DocumentSource}: fetches the user's public RSS feed and
 * renders one Markdown section per article. An empty feed (no posts yet) simply
 * indexes zero chunks — nothing breaks, and posts appear automatically on the next
 * re-index once published. Only created when {@code app.medium.enabled=true}.
 */
@Component
@Order(3)
@ConditionalOnProperty(prefix = "app.medium", name = "enabled", havingValue = "true")
public class MediumDocumentSource implements DocumentSource {

    private final MediumApi mediumApi;
    private final MediumFeedParser parser;
    private final String handle;

    public MediumDocumentSource(MediumApi mediumApi, MediumFeedParser parser,
                                @Value("${app.medium.username}") String handle) {
        this.mediumApi = mediumApi;
        this.parser = parser;
        this.handle = handle;
    }

    @Override
    public String name() {
        return "Medium posts (" + handle + ")";
    }

    @Override
    public String markdown() {
        return parser.parse(mediumApi.fetchFeed(handle)).stream()
                .map(MediumPostFormatter::format)
                .collect(Collectors.joining("\n\n"));
    }
}
