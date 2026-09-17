package com.example.shortlink.observability;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AwsClientTracingTest {

    @Test
    void noSegmentMeansNoSqsTraceAttribute() {
        assertThat(AwsClientTracing.currentTraceHeader()).isEmpty();
        assertThat(AwsClientTracing.sqsTraceAttributes()).isEmpty();
    }
}
