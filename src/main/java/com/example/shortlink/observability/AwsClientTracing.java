package com.example.shortlink.observability;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.interceptors.TracingInterceptor;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;

/**
 * Instruments AWS SDK v2 clients so DynamoDB and SQS calls become X-Ray
 * subsegments under the Lambda segment. Missing-context is ignored so local
 * tests and {@code spring-boot:run} do not fail without an X-Ray daemon.
 */
public final class AwsClientTracing {

    static {
        AWSXRay.getGlobalRecorder().setContextMissingStrategy(new IgnoreErrorContextMissingStrategy());
    }

    private AwsClientTracing() {
    }

    public static ClientOverrideConfiguration overrideConfig() {
        return ClientOverrideConfiguration.builder()
                .addExecutionInterceptor(new TracingInterceptor())
                .build();
    }
}
