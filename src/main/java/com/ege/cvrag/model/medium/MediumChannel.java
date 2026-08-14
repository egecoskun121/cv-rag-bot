package com.ege.cvrag.model.medium;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** The {@code <channel>} element — {@code <item>}s are direct, unwrapped siblings. */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class MediumChannel {

    @JacksonXmlElementWrapper(useWrapping = false)
    @JacksonXmlProperty(localName = "item")
    private List<MediumItem> items;
}
