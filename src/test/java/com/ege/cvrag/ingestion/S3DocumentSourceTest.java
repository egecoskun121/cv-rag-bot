package com.ege.cvrag.ingestion;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test with a mocked {@link S3Client} — no AWS account or network needed, so
 * it is CI-safe. It verifies the getObject → UTF-8 mapping and error wrapping;
 * real end-to-end behaviour against a live bucket is covered by the (backlogged)
 * LocalStack/Testcontainers integration test.
 */
class S3DocumentSourceTest {

    @Test
    void readsMarkdownFromS3Object() {
        S3Client s3 = mock(S3Client.class);
        ResponseBytes<GetObjectResponse> body = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(),
                "# CV\nHand-rolled RAG.".getBytes(StandardCharsets.UTF_8));
        when(s3.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(body);

        S3DocumentSource source = new S3DocumentSource(s3, "ege-cv-rag-docs", "cv.md");

        assertThat(source.markdown()).isEqualTo("# CV\nHand-rolled RAG.");
        assertThat(source.name()).isEqualTo("CV (s3://ege-cv-rag-docs/cv.md)");
    }

    @Test
    void wrapsS3FailureInIllegalState() {
        S3Client s3 = mock(S3Client.class);
        when(s3.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().message("missing").build());

        S3DocumentSource source = new S3DocumentSource(s3, "ege-cv-rag-docs", "cv.md");

        assertThatThrownBy(source::markdown)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("s3://ege-cv-rag-docs/cv.md");
    }
}
