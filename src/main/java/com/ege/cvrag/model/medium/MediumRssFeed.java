package com.ege.cvrag.model.medium;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/** Root {@code <rss>} element of a Medium profile feed. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "rss")
public class MediumRssFeed {

    private MediumChannel channel;

    public MediumChannel getChannel() {
        return channel;
    }

    public void setChannel(MediumChannel channel) {
        this.channel = channel;
    }
}
