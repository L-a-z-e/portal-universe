package com.portal.universe.shoppingservice.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

import java.net.URI;

@Configuration
public class EventBridgeConfig {

    @Value("${spring.cloud.aws.endpoint:}")
    private String awsEndpoint;

    @Value("${spring.cloud.aws.region.static:ap-northeast-2}")
    private String region;

    @Value("${spring.cloud.aws.credentials.access-key:}")
    private String accessKey;

    @Value("${spring.cloud.aws.credentials.secret-key:}")
    private String secretKey;

    @Bean
    public EventBridgeClient eventBridgeClient() {
        var builder = EventBridgeClient.builder()
                .region(Region.of(region));

        if (!awsEndpoint.isBlank()) {
            builder.endpointOverride(URI.create(awsEndpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)));
        }

        return builder.build();
    }
}
