package com.example.shortlink.events;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.shortlink.persistence.DynamoDbClickIncrement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * SQS worker: not Spring MVC. Same jar, different entry point. Increments
 * clickCount so the HTTP Lambda can return the 302 without waiting on DynamoDB.
 */
public class ClickEventHandler implements RequestHandler<SQSEvent, Void> {

    private static final Logger log = LoggerFactory.getLogger(ClickEventHandler.class);

    private final DynamoDbClient dynamoDb;
    private final String tableName;

    public ClickEventHandler() {
        this(DynamoDbClient.builder()
                .httpClient(UrlConnectionHttpClient.builder().build())
                .build(),
                System.getenv("TABLE_NAME"));
    }

    ClickEventHandler(DynamoDbClient dynamoDb, String tableName) {
        this.dynamoDb = dynamoDb;
        this.tableName = tableName;
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            ClickEvent click = ClickEvent.fromJson(message.getBody());
            boolean updated = DynamoDbClickIncrement.increment(dynamoDb, tableName, click.code()).isPresent();
            if (!updated) {
                log.warn("Click for unknown code {}, dropping message", click.code());
            }
        }
        return null;
    }
}
