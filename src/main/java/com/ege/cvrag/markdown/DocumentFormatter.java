package com.ege.cvrag.markdown;

/**
 * Formats a domain object (a GitHub repo, a Medium post, ...) into the Markdown
 * section {@code MarkdownIndexer} chunks. Every source-specific formatter
 * implements this so the contract is uniform, even though each source's input
 * shape differs.
 */
public interface DocumentFormatter<T> {

    String format(T item);
}
