package com.ege.cvrag.model.medium;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

/** The {@code <channel>} element — {@code <item>}s are direct, unwrapped siblings. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediumChannel {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "item")
    private List<MediumItem> items;

    public List<MediumItem> getItems() {
        return items;
    }

    public void setItems(List<MediumItem> items) {
        this.items = items;
    }
}
