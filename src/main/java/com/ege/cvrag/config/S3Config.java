package com.ege.cvrag.config;

import com.ege.cvrag.constant.RagBotConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Builds the {@link S3Client} only when the CV is sourced from S3
 * ({@code app.docs.source=s3}), so the AWS SDK is never touched in the default
 * classpath setup (and CI needs no AWS credentials).
 *
 * Credentials come from the AWS default provider chain — nothing is stored here.
 */
@Configuration
@ConditionalOnProperty(prefix = "app.docs", name = "source",
        havingValue = RagBotConstants.DOCS_SOURCE_S3)
public class S3Config {

    @Bean
    public S3Client s3Client(@Value("${app.docs.s3.region}") String region) {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
