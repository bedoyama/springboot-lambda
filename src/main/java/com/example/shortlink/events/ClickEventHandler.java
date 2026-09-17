package com.example.shortlink.events;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.shortlink.observability.AwsClientTracing;
import com.example.shortlink.observability.MdcKeys;
import com.example.shortlink.observability.ShortLinkMetrics;
import com.example.shortlink.persistence.DynamoDbClickIncrement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
                .overrideConfiguration(AwsClientTracing.overrideConfig())
                .build(),
                System.getenv("TABLE_NAME"));
    }

    ClickEventHandler(DynamoDbClient dynamoDb, String tableName) {
        this.dynamoDb = dynamoDb;
        this.tableName = tableName;
    }

    static String sqsTraceHeader(SQSEvent.SQSMessage message) {
        if (message.getMessageAttributes() == null) {
            return null;
        }
        SQSEvent.MessageAttribute attribute =
                message.getMessageAttributes().get(AwsClientTracing.SQS_TRACE_HEADER);
        return attribute == null ? null : attribute.getStringValue();
    }

    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            ClickEvent click = ClickEvent.fromJson(message.getBody());
            String traceHeader = sqsTraceHeader(message);
            if (click.correlationId() != null) {
                MDC.put(MdcKeys.CORRELATION_ID, click.correlationId());
            }
            try {
                log.info(
                        "Recording click code={} correlationId={} messageId={} traceHeader={}",
                        click.code(),
                        click.correlationId(),
                        message.getMessageId(),
                        traceHeader);
                boolean updated = DynamoDbClickIncrement.increment(dynamoDb, tableName, click.code()).isPresent();
                if (updated) {
                    ShortLinkMetrics.clickRecorded();
                } else {
                    ShortLinkMetrics.unknownClickCode();
                    log.warn(
                            "Click for unknown code {} correlationId={}, dropping message",
                            click.code(),
                            click.correlationId());
                }
            } finally {
                MDC.remove(MdcKeys.CORRELATION_ID);
            }
        }
        return null;
    }
}
