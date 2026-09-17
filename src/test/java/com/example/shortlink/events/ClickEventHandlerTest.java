package com.example.shortlink.events;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.shortlink.domain.Link;
import com.example.shortlink.persistence.DynamoDbClientHolder;
import com.example.shortlink.persistence.LinkRepository;
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
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceInUseException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
@ActiveProfiles("lambda")
class ClickEventHandlerTest {

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
        registry.add("app.sqs.queue-url", () -> "https://localhost/queue");
    }

    @Autowired
    private DynamoDbClientHolder dynamoDbClient;

    @Autowired
    private LinkRepository links;

    @BeforeEach
    void createTable() {
        try {
            dynamoDbClient.client().createTable(request -> request
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
            dynamoDbClient.client().waiter().waitUntilTableExists(request -> request.tableName(TABLE));
        } catch (ResourceInUseException ignored) {
            // already created
        }
    }

    @Test
    void sqsMessageIncrementsClickCount() {
        String code = UUID.randomUUID().toString().substring(0, 7);
        links.create(new Link(code, "https://example.com/click", Instant.parse("2026-09-17T12:00:00Z"), 0));

        SQSEvent event = new SQSEvent();
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setBody(ClickEvent.now(code, "corr-from-api").toJson());
        event.setRecords(List.of(message));

        new ClickEventHandler(dynamoDbClient.client(), TABLE).handleRequest(event, null);

        assertThat(links.findByCode(code))
                .hasValueSatisfying(link -> assertThat(link.clickCount()).isEqualTo(1));
    }
}
