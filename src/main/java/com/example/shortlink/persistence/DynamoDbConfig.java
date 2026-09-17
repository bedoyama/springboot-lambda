package com.example.shortlink.persistence;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * DynamoDB fits Lambda: no connection pool to keep warm, IAM auth, per-request billing.
 * RDS + Hikari would hold connections across freeze/thaw and exhaust the database.
 *
 * <p>The holder is a Spring singleton so warm invokes reuse the client. After a
 * SnapStart restore it builds a fresh client. UrlConnection is the light HTTP
 * client AWS recommends for Lambda, instead of Netty.
 */
@Configuration
@Profile("!local")
public class DynamoDbConfig {

    @Bean(destroyMethod = "close")
    DynamoDbClientHolder dynamoDbClientHolder(
            @Value("${app.dynamodb.endpoint:}") String endpoint,
            @Value("${app.dynamodb.region:us-east-1}") String region) {
        return new DynamoDbClientHolder(endpoint, region);
    }
}
