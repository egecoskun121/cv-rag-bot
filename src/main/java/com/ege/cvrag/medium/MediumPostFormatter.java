package com.ege.cvrag.medium;

import com.ege.cvrag.model.medium.MediumItem;
import org.jsoup.Jsoup;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Formats a Medium RSS item into a Markdown section the RAG pipeline can index.
 * Pure — no I/O — so it is easy to test. Strips the article's HTML down to plain
 * text with Jsoup; raw tags would just add noise to the embedding.
 */
public final class MediumPostFormatter {

    private MediumPostFormatter() {
        // utility class
    }

    public static String format(MediumItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Blog: ").append(item.getTitle()).append('\n');
        sb.append(plainText(item.getContent())).append("\n\n");
        if (Objects.nonNull(item.getPubDate())) {
            sb.append("- Published: ").append(datePart(item.getPubDate())).append('\n');
        }
        if (Objects.nonNull(item.getLink())) {
            sb.append("- Link: ").append(item.getLink()).append('\n');
        }
        return sb.toString().strip();
    }

    private static String plainText(String html) {
        return Objects.isNull(html) ? "" : Jsoup.parse(html).text();
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
