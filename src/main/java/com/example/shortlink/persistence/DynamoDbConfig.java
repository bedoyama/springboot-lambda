package com.example.shortlink.persistence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * DynamoDB fits Lambda: no connection pool to keep warm, IAM auth, per-request billing.
 * RDS + Hikari would hold connections across freeze/thaw and exhaust the database.
 *
 * <p>The client is a Spring singleton so warm invokes reuse it. UrlConnection is the
 * light HTTP client AWS recommends for Lambda, instead of Netty.
 */
@Configuration
@Profile("!local")
public class DynamoDbConfig {

    @Bean(destroyMethod = "close")
    DynamoDbClient dynamoDbClient(
            @Value("${app.dynamodb.endpoint:}") String endpoint,
            @Value("${app.dynamodb.region:us-east-1}") String region) {
        var builder = DynamoDbClient.builder()
                .httpClient(UrlConnectionHttpClient.builder().build());
        if (!endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("local", "local")));
        }
        return builder.build();
    }
}
