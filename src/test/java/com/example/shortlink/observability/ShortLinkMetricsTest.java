package com.example.shortlink.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ShortLinkMetricsTest {

    @Test
    void emitDoesNotThrow() {
        assertThatCode(() -> ShortLinkMetrics.redirect(12)).doesNotThrowAnyException();
        assertThatCode(ShortLinkMetrics::clickRecorded).doesNotThrowAnyException();
        assertThatCode(ShortLinkMetrics::unknownClickCode).doesNotThrowAnyException();
    }
}
