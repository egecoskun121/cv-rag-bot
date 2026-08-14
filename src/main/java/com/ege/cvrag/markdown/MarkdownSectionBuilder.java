package com.ege.cvrag.markdown;

import java.util.Objects;

/**
 * Fluent builder for the "## Type: Title\n\nbody\n- Label: value..." shape every
 * {@code DocumentSource} formatter (GitHub, Medium, ...) renders. Centralizes the
 * null/blank-skipping bullet logic so a new formatter doesn't repeat it.
 */
public final class MarkdownSectionBuilder {

    private final StringBuilder sb = new StringBuilder();

    private MarkdownSectionBuilder() {
        // use heading(...)
    }

    public static MarkdownSectionBuilder heading(String type, String title) {
        MarkdownSectionBuilder builder = new MarkdownSectionBuilder();
        builder.sb.append("## ").append(type).append(": ").append(title).append('\n');
        return builder;
    }

    /** The section's main text (description/article body). Skipped when blank. */
    public MarkdownSectionBuilder body(String text) {
        if (Objects.nonNull(text) && !text.isBlank()) {
            sb.append(text).append("\n\n");
        }
        return this;
    }

    /** A "- Label: value" bullet, skipped entirely when the value is null/blank. */
    public MarkdownSectionBuilder field(String label, String value) {
        if (Objects.nonNull(value) && !value.isBlank()) {
            sb.append("- ").append(label).append(": ").append(value).append('\n');
        }
        return this;
    }

    public String build() {
        return sb.toString().strip();
    }
}
