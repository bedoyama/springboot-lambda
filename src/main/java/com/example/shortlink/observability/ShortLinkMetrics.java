package com.example.shortlink.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;

/**
 * CloudWatch Embedded Metric Format (EMF): one JSON log line, extracted into
 * metrics. Cheaper and faster than {@code PutMetricData} on the request path.
 *
 * <p>Only low-cardinality dimensions (here {@code function}). Never use
 * {@code code} as a dimension — each unique value is a new time series.
 */
public final class ShortLinkMetrics {

    public static final String NAMESPACE = "ShortLink";
    public static final String FUNCTION_API = "api";
    public static final String FUNCTION_WORKER = "worker";
    public static final String REDIRECTS = "Redirects";
    public static final String REDIRECT_DURATION = "RedirectDuration";
    public static final String CLICKS_RECORDED = "ClicksRecorded";
    public static final String UNKNOWN_CLICK_CODE = "UnknownClickCode";

    private static final Logger log = LoggerFactory.getLogger(ShortLinkMetrics.class);

    private ShortLinkMetrics() {
    }

    public static void redirect(long durationMs) {
        try {
            MetricsLogger metrics = new MetricsLogger();
            metrics.setNamespace(NAMESPACE);
            metrics.putDimensions(DimensionSet.of("function", FUNCTION_API));
            metrics.putMetric(REDIRECTS, 1, Unit.COUNT);
            metrics.putMetric(REDIRECT_DURATION, durationMs, Unit.MILLISECONDS);
            metrics.flush();
        } catch (RuntimeException e) {
            log.warn("Failed to emit redirect metrics", e);
        }
    }

    public static void clickRecorded() {
        count(FUNCTION_WORKER, CLICKS_RECORDED);
    }

    public static void unknownClickCode() {
        count(FUNCTION_WORKER, UNKNOWN_CLICK_CODE);
    }

    static void count(String function, String metricName) {
        try {
            MetricsLogger metrics = new MetricsLogger();
            metrics.setNamespace(NAMESPACE);
            metrics.putDimensions(DimensionSet.of("function", function));
            metrics.putMetric(metricName, 1, Unit.COUNT);
            metrics.flush();
        } catch (RuntimeException e) {
            log.warn("Failed to emit metric {}", metricName, e);
        }
    }
}
