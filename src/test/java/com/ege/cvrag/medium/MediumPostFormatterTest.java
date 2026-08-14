package com.ege.cvrag.medium;

import com.ege.cvrag.model.medium.MediumItem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MediumPostFormatterTest {

    @Test
    void formatsPostAsMarkdownWithPlainTextBody() {
        MediumItem item = new MediumItem();
        item.setTitle("Building a RAG Bot From Scratch");
        item.setContent("<p>Full <b>article</b> body here.</p>");
        item.setPubDate("Fri, 14 Aug 2026 12:00:00 GMT");
        item.setLink("https://medium.com/@egecoskun/building-a-rag-bot-abc123");

        String md = MediumPostFormatter.format(item);

        assertThat(md).startsWith("## Blog: Building a RAG Bot From Scratch");
        assertThat(md).contains("Full article body here.");   // HTML tags stripped
        assertThat(md).doesNotContain("<b>");
        assertThat(md).contains("- Published: 2026-08-14");   // RFC-1123 -> date-only
        assertThat(md).contains("- Link: https://medium.com/@egecoskun/building-a-rag-bot-abc123");
    }

    @Test
    void toleratesMissingOptionalFields() {
        MediumItem item = new MediumItem();
        item.setTitle("Draft Thoughts");
        item.setContent(null);

        String md = MediumPostFormatter.format(item);

        assertThat(md).isEqualTo("## Blog: Draft Thoughts");
    }

    @Test
    void fallsBackToRawStringOnUnparsableDate() {
        MediumItem item = new MediumItem();
        item.setTitle("Post");
        item.setPubDate("not-a-date");

        assertThat(MediumPostFormatter.format(item)).contains("- Published: not-a-date");
    }
}
