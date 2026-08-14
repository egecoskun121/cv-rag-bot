package com.ege.cvrag.medium;

import com.ege.cvrag.model.medium.MediumChannel;
import com.ege.cvrag.model.medium.MediumItem;
import com.ege.cvrag.model.medium.MediumRssFeed;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/** Parses raw Medium RSS/XML into {@link MediumItem}s. Isolated so it's easy to unit-test with a fixed XML string. */
@Component
public class MediumFeedParser {

    private final XmlMapper xmlMapper = XmlMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    public List<MediumItem> parse(String rssXml) {
        try {
            MediumRssFeed feed = xmlMapper.readValue(rssXml, MediumRssFeed.class);
            MediumChannel channel = feed.getChannel();
            List<MediumItem> items = Objects.isNull(channel) ? null : channel.getItems();
            return Objects.isNull(items) ? List.of() : items;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Medium RSS feed", e);
        }
    }
}
