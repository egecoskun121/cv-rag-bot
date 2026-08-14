package com.ege.cvrag.model.medium;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

/**
 * One {@code <item>} in a Medium RSS feed. A plain class rather than a record —
 * Jackson XML binds namespaced elements (Medium's {@code content:encoded}) and
 * repeated non-wrapped elements more reliably onto mutable fields than onto a
 * record's canonical constructor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediumItem {

    @JacksonXmlProperty(localName = "title")
    private String title;

    @JacksonXmlProperty(localName = "link")
    private String link;

    @JacksonXmlProperty(localName = "pubDate")
    private String pubDate;

    /** Full HTML article body — Medium's free RSS includes it, not just a summary. */
    @JacksonXmlProperty(localName = "encoded")
    private String content;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getPubDate() {
        return pubDate;
    }

    public void setPubDate(String pubDate) {
        this.pubDate = pubDate;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
