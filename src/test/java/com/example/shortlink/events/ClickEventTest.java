package com.example.shortlink.events;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClickEventTest {

    @Test
    void roundTripWithoutCorrelationId() {
        ClickEvent original = ClickEvent.now("abc1234");
        ClickEvent parsed = ClickEvent.fromJson(original.toJson());
        assertThat(parsed.code()).isEqualTo("abc1234");
        assertThat(parsed.clickedAt()).isEqualTo(original.clickedAt());
        assertThat(parsed.correlationId()).isNull();
    }

    @Test
    void roundTripWithCorrelationId() {
        ClickEvent parsed = ClickEvent.fromJson(ClickEvent.now("abc1234", "corr-1").toJson());
        assertThat(parsed.code()).isEqualTo("abc1234");
        assertThat(parsed.correlationId()).isEqualTo("corr-1");
    }

    @Test
    void fromJsonAcceptsLegacyPayload() {
        ClickEvent parsed = ClickEvent.fromJson(
                "{\"code\":\"xyz9876\",\"clickedAt\":\"2026-09-17T12:00:00Z\"}");
        assertThat(parsed.code()).isEqualTo("xyz9876");
        assertThat(parsed.correlationId()).isNull();
    }
}
