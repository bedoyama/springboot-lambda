package com.example.shortlink;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.example.shortlink.observability.MdcKeys;
import org.slf4j.MDC;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Lambda entry point. API Gateway JSON events are turned into servlet requests
 * and dispatched to the same {@code @RestController} beans used locally.
 *
 * <p>The Spring context is built once in a static block: a cold start pays for
 * Boot initialization, later (warm) invokes reuse the handler.
 */
public class StreamLambdaHandler implements RequestStreamHandler {

    private static final SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> HANDLER;

    static {
        try {
            HANDLER = SpringBootLambdaContainerHandler.getAwsProxyHandler(ShortlinkApplication.class);
        } catch (ContainerInitializationException e) {
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        if (context != null && context.getAwsRequestId() != null) {
            MDC.put(MdcKeys.REQUEST_ID, context.getAwsRequestId());
        }
        try {
            HANDLER.proxyStream(input, output, context);
        } finally {
            MDC.clear();
        }
    }
}
