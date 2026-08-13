package com.ege.cvrag.ingestion;

import com.ege.cvrag.constant.RagBotConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

/**
 * The CV document source that reads {@code cv.md} from an AWS S3 object instead
 * of the local classpath. Active when {@code app.docs.source=s3}; it and
 * {@link CvDocumentSource} are mutually exclusive (same {@code @Order(1)} slot),
 * so the ingestion orchestrator is oblivious to where the CV comes from.
 *
 * Credentials are resolved by the AWS default provider chain (env vars, profile,
 * container/instance role) — none are hard-coded here.
 */
@Component
@Order(1)
@ConditionalOnProperty(prefix = "app.docs", name = "source",
        havingValue = RagBotConstants.DOCS_SOURCE_S3)
public class S3DocumentSource implements DocumentSource {

    private final S3Client s3;
    private final String bucket;
    private final String key;

    public S3DocumentSource(S3Client s3,
                            @Value("${app.docs.s3.bucket}") String bucket,
                            @Value("${app.docs.s3.key}") String key) {
        this.s3 = s3;
        this.bucket = bucket;
        this.key = key;
    }

    @Override
    public String name() {
        return "CV (" + RagBotConstants.S3_URI_PREFIX + bucket + "/" + key + ")";
    }

    @Override
    public String markdown() {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        try {
            return s3.getObjectAsBytes(request).asUtf8String();
        } catch (SdkException e) {
            throw new IllegalStateException(
                    RagBotConstants.ERROR_S3_READ + RagBotConstants.S3_URI_PREFIX + bucket + "/" + key, e);
        }
    }
}
