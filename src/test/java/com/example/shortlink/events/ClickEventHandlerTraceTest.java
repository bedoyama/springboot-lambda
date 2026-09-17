package com.example.shortlink.events;

import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.example.shortlink.observability.AwsClientTracing;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClickEventHandlerTraceTest {

    @Test
    void readsAwsTraceHeaderFromMessage() {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        SQSEvent.MessageAttribute attribute = new SQSEvent.MessageAttribute();
        attribute.setStringValue("Root=1-example;Parent=abc;Sampled=1");
        message.setMessageAttributes(Map.of(AwsClientTracing.SQS_TRACE_HEADER, attribute));

        assertThat(ClickEventHandler.sqsTraceHeader(message))
                .isEqualTo("Root=1-example;Parent=abc;Sampled=1");
    }

    @Test
    void missingTraceHeaderIsNull() {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        assertThat(ClickEventHandler.sqsTraceHeader(message)).isNull();
    }
}
