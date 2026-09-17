package com.example.shortlink.observability;

import com.amazonaws.xray.AWSXRay;
import com.amazonaws.xray.entities.TraceHeader;
import com.amazonaws.xray.interceptors.TracingInterceptor;
import com.amazonaws.xray.strategy.IgnoreErrorContextMissingStrategy;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;
import java.util.Optional;

/**
 * Instruments AWS SDK v2 clients so DynamoDB and SQS calls become X-Ray
 * subsegments under the Lambda segment. Missing-context is ignored so local
 * tests and {@code spring-boot:run} do not fail without an X-Ray daemon.
 */
public final class AwsClientTracing {

    public static final String SQS_TRACE_HEADER = "AWSTraceHeader";

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

    /**
     * X-Ray header for the current Lambda segment, or {@code _X_AMZN_TRACE_ID}.
     * SQS consumers (including Lambda) continue the trace when this is set as
     * the {@code AWSTraceHeader} message attribute.
     */
    public static Optional<String> currentTraceHeader() {
        return AWSXRay.getCurrentSegmentOptional()
                .map(segment -> TraceHeader.fromEntity(segment).toString())
                .or(() -> Optional.ofNullable(System.getenv("_X_AMZN_TRACE_ID"))
                        .filter(header -> !header.isBlank()));
    }

    public static Map<String, MessageAttributeValue> sqsTraceAttributes() {
        return currentTraceHeader()
                .map(header -> Map.of(
                        SQS_TRACE_HEADER,
                        MessageAttributeValue.builder()
                                .dataType("String")
                                .stringValue(header)
                                .build()))
                .orElseGet(Map::of);
    }
}
