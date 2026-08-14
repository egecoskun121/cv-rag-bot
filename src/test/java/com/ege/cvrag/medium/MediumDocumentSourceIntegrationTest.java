package com.ege.cvrag.medium;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: real {@link MediumDocumentSource} + {@link MediumApi} +
 * {@link MediumFeedParser} against an in-process RSS stub — no network, no real
 * Medium account needed.
 */
class MediumDocumentSourceIntegrationTest {

    private static final String FEED_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss xmlns:content="http://purl.org/rss/1.0/modules/content/" version="2.0">
              <channel>
                <title>Stories by Ege Coşkun on Medium</title>
                <item>
                  <title><![CDATA[Learning RAG the Hard Way]]></title>
                  <link>https://medium.com/@egecoskun/learning-rag</link>
                  <pubDate>Fri, 14 Aug 2026 12:00:00 GMT</pubDate>
                  <content:encoded><![CDATA[<p>Notes from building a RAG bot.</p>]]></content:encoded>
                </item>
              </channel>
            </rss>""";

    private static final String EMPTY_FEED_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <rss version="2.0"><channel><title>No posts yet</title></channel></rss>""";

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (Objects.nonNull(server)) {
            server.stop(0);
        }
    }

    @Test
    void indexesPostsFromTheFeed() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/@egecoskun", exchange -> respond(exchange, FEED_XML));
        server.start();

        MediumDocumentSource source = new MediumDocumentSource(
                mediumApiFor("http://localhost:" + server.getAddress().getPort()),
                new MediumFeedParser(), "@egecoskun");

        String markdown = source.markdown();

        assertThat(markdown).contains("## Blog: Learning RAG the Hard Way");
        assertThat(markdown).contains("Notes from building a RAG bot.");
    }

    @Test
    void emptyFeedIndexesNothingWithoutError() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/@egecoskun", exchange -> respond(exchange, EMPTY_FEED_XML));
        server.start();

        MediumDocumentSource source = new MediumDocumentSource(
                mediumApiFor("http://localhost:" + server.getAddress().getPort()),
                new MediumFeedParser(), "@egecoskun");

        assertThat(source.markdown()).isEmpty();
    }

    private MediumApi mediumApiFor(String baseUrl) {
        RestClient client = RestClient.builder().baseUrl(baseUrl).build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(client))
                .build()
                .createClient(MediumApi.class);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/rss+xml");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
