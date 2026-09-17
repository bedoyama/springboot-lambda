package com.example.shortlink.events;

import com.example.shortlink.domain.ClickRecorder;
import com.example.shortlink.observability.AwsClientTracing;
import com.example.shortlink.observability.MdcKeys;
import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Component
@Profile("!local")
public class SqsClickRecorder implements ClickRecorder, Resource, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(SqsClickRecorder.class);

    private final String queueUrl;
    private volatile SqsClient sqs;

    public SqsClickRecorder(@Value("${app.sqs.queue-url}") String queueUrl) {
        this.queueUrl = queueUrl;
        this.sqs = buildClient();
        Core.getGlobalContext().register(this);
    }

    @Override
    public void record(String code) {
        sqs.sendMessage(request -> request
                .queueUrl(queueUrl)
                .messageBody(ClickEvent.now(code, MDC.get(MdcKeys.CORRELATION_ID)).toJson()));
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) {
        closeClient();
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) {
        sqs = buildClient();
        log.info("SnapStart restore: rebuilt SqsClient");
    }

    @Override
    public void close() {
        closeClient();
    }

    private static SqsClient buildClient() {
        return SqsClient.builder()
                .httpClient(UrlConnectionHttpClient.builder().build())
                .overrideConfiguration(AwsClientTracing.overrideConfig())
                .build();
    }

    private void closeClient() {
        SqsClient current = sqs;
        sqs = null;
        if (current != null) {
            current.close();
        }
    }
}
