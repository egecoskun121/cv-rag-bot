package com.ege.cvrag.ingestion;

import com.ege.cvrag.model.ingestion.IngestionSummary;
import com.ege.cvrag.vectorstore.PgVectorStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentIngestionTest {

    /** A trivial source; a null markdown makes {@code markdown()} throw to test skipping. */
    private record FakeSource(String name, String markdown) implements DocumentSource {
        @Override
        public String markdown() {
            if (markdown == null) {
                throw new IllegalStateException("boom");
            }
            return markdown;
        }
    }

    @Test
    void indexesEverySourceAndSumsChunks() {
        MarkdownIndexer indexer = mock(MarkdownIndexer.class);
        PgVectorStore store = mock(PgVectorStore.class);
        when(indexer.index("cv")).thenReturn(14);
        when(indexer.index("gh")).thenReturn(20);

        DocumentIngestion ingestion = new DocumentIngestion(
                List.of(new FakeSource("CV", "cv"), new FakeSource("GitHub", "gh")),
                indexer, store, true);

        IngestionSummary summary = ingestion.reindex();

        verify(store).deleteAll();                         // reload cleared first
        assertThat(summary.totalChunks()).isEqualTo(34);
        assertThat(summary.sources()).hasSize(2);
        assertThat(summary.sources().get(0).status()).isEqualTo("indexed");
        assertThat(summary.sources().get(0).chunks()).isEqualTo(14);
    }

    @Test
    void skipsAFailingSourceButKeepsTheRest() {
        MarkdownIndexer indexer = mock(MarkdownIndexer.class);
        PgVectorStore store = mock(PgVectorStore.class);
        when(indexer.index("cv")).thenReturn(14);

        DocumentIngestion ingestion = new DocumentIngestion(
                List.of(new FakeSource("CV", "cv"), new FakeSource("Broken", null)),
                indexer, store, true);

        IngestionSummary summary = ingestion.reindex();

        assertThat(summary.totalChunks()).isEqualTo(14);   // broken source contributed 0
        assertThat(summary.sources().get(1).chunks()).isZero();
        assertThat(summary.sources().get(1).status()).startsWith("skipped:");
    }

    @Test
    void doesNotClearWhenReloadDisabled() {
        MarkdownIndexer indexer = mock(MarkdownIndexer.class);
        PgVectorStore store = mock(PgVectorStore.class);
        when(indexer.index(anyString())).thenReturn(1);

        new DocumentIngestion(List.of(new FakeSource("CV", "cv")), indexer, store, false)
                .reindex();

        verify(store, never()).deleteAll();
    }
}
