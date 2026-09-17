package com.example.shortlink.persistence;

import com.example.shortlink.domain.Link;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("lambda")
class DynamoDbLinkRepositoryTest {

    private static final String TABLE = "ShortLinks";

    @Container
    static GenericContainer<?> dynamoDb = new GenericContainer<>(
            DockerImageName.parse("amazon/dynamodb-local:2.6.1"))
            .withExposedPorts(8000);

    @DynamicPropertySource
    static void dynamoProperties(DynamicPropertyRegistry registry) {
        registry.add("app.dynamodb.endpoint",
                () -> "http://" + dynamoDb.getHost() + ":" + dynamoDb.getMappedPort(8000));
        registry.add("app.dynamodb.table-name", () -> TABLE);
    }

    @Autowired
    private DynamoDbClient dynamoDbClient;

    @Autowired
    private LinkRepository links;

    @BeforeEach
    void createTable() {
        try {
            dynamoDbClient.createTable(request -> request
                    .tableName(TABLE)
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("code")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("code")
                            .keyType(KeyType.HASH)
                            .build())
                    .billingMode(BillingMode.PAY_PER_REQUEST));
            dynamoDbClient.waiter().waitUntilTableExists(request -> request.tableName(TABLE));
        } catch (ResourceInUseException ignored) {
            // table already exists from a previous test in this class
        }
    }

    @Test
    void createFindAndIncrementClicks() {
        String code = UUID.randomUUID().toString().substring(0, 7);
        Link created = new Link(code, "https://example.com/ddb", Instant.parse("2026-09-17T12:00:00Z"), 0);

        assertThat(links.create(created)).isTrue();
        assertThat(links.create(created)).isFalse();

        Optional<Link> found = links.findByCode(code);
        assertThat(found).hasValueSatisfying(link -> {
            assertThat(link.originalUrl()).isEqualTo("https://example.com/ddb");
            assertThat(link.clickCount()).isZero();
        });

        assertThat(links.incrementClicks(code))
                .hasValueSatisfying(link -> assertThat(link.clickCount()).isEqualTo(1));
        assertThat(links.findByCode(code))
                .hasValueSatisfying(link -> assertThat(link.clickCount()).isEqualTo(1));
    }

    @Test
    void missingCodeIsEmpty() {
        assertThat(links.findByCode("missing")).isEmpty();
        assertThat(links.incrementClicks("missing")).isEmpty();
    }
}
