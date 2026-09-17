package com.example.shortlink.persistence;

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;

/**
 * Holds the DynamoDB client so SnapStart can close it before the snapshot and
 * rebuild it after restore. Connections and cached credentials in the snapshot
 * are not safe to reuse.
 */
public class DynamoDbClientHolder implements Resource, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DynamoDbClientHolder.class);

    private final String endpoint;
    private final String region;
    private volatile DynamoDbClient client;

    public DynamoDbClientHolder(String endpoint, String region) {
        this.endpoint = endpoint;
        this.region = region;
        this.client = buildClient();
        Core.getGlobalContext().register(this);
    }

    public DynamoDbClient client() {
        return client;
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) {
        closeClient();
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) {
        client = buildClient();
        log.info("SnapStart restore: rebuilt DynamoDbClient");
    }

    @Override
    public void close() {
        closeClient();
    }

    private DynamoDbClient buildClient() {
        var builder = DynamoDbClient.builder()
                .httpClient(UrlConnectionHttpClient.builder().build());
        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint))
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("local", "local")));
        }
        return builder.build();
    }

    private void closeClient() {
        DynamoDbClient current = client;
        client = null;
        if (current != null) {
            current.close();
        }
    }
}
