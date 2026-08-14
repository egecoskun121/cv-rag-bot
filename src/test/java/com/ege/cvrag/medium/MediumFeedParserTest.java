package com.ege.cvrag.medium;

import com.ege.cvrag.model.medium.MediumItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MediumFeedParserTest {

    private static final String FEED_WITH_ONE_POST = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss xmlns:content="http://purl.org/rss/1.0/modules/content/" version="2.0">
              <channel>
                <title>Stories by Ege Coşkun on Medium</title>
                <item>
                  <title><![CDATA[Building a RAG Bot From Scratch]]></title>
                  <link>https://medium.com/@egecoskun/building-a-rag-bot-abc123</link>
                  <pubDate>Fri, 14 Aug 2026 12:00:00 GMT</pubDate>
                  <content:encoded><![CDATA[<p>Full <b>article</b> body here.</p>]]></content:encoded>
                </item>
              </channel>
            </rss>""";

    private static final String EMPTY_FEED = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0">
              <channel>
                <title>Stories by Ege Coşkun on Medium</title>
              </channel>
            </rss>""";

    private final MediumFeedParser parser = new MediumFeedParser();

    @Test
    void parsesItemFieldsIncludingNamespacedContent() {
        List<MediumItem> items = parser.parse(FEED_WITH_ONE_POST);

        assertThat(items).hasSize(1);
        MediumItem item = items.get(0);
        assertThat(item.getTitle()).isEqualTo("Building a RAG Bot From Scratch");
        assertThat(item.getLink()).isEqualTo("https://medium.com/@egecoskun/building-a-rag-bot-abc123");
        assertThat(item.getPubDate()).isEqualTo("Fri, 14 Aug 2026 12:00:00 GMT");
        assertThat(item.getContent()).contains("Full <b>article</b> body here.");
    }

    @Test
    void emptyFeedReturnsEmptyList() {
        assertThat(parser.parse(EMPTY_FEED)).isEmpty();
    }
}
