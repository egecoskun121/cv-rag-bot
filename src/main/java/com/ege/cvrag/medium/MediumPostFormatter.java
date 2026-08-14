package com.ege.cvrag.medium;

import com.ege.cvrag.markdown.DocumentFormatter;
import com.ege.cvrag.markdown.MarkdownSectionBuilder;
import com.ege.cvrag.model.medium.MediumItem;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Formats a Medium RSS item into a Markdown section the RAG pipeline can index.
 * Pure — no I/O — so it is easy to test. Strips the article's HTML down to plain
 * text with Jsoup; raw tags would just add noise to the embedding.
 */
@Component
public class MediumPostFormatter implements DocumentFormatter<MediumItem> {

    @Override
    public String format(MediumItem item) {
        return MarkdownSectionBuilder.heading("Blog", item.getTitle())
                .body(plainText(item.getContent()))
                .field("Published", publishedDate(item.getPubDate()))
                .field("Link", item.getLink())
                .build();
    }

    private static String plainText(String html) {
        return Objects.isNull(html) ? "" : Jsoup.parse(html).text();
    }

    private static String publishedDate(String pubDate) {
        return Objects.isNull(pubDate) ? null : datePart(pubDate);
    }

    /** Medium's pubDate is RFC-1123, e.g. "Fri, 14 Aug 2026 12:00:00 GMT" -> "2026-08-14". */
    private static String datePart(String pubDate) {
        try {
            return ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME).toLocalDate().toString();
        } catch (DateTimeParseException e) {
            return pubDate;
        }
    }
}
