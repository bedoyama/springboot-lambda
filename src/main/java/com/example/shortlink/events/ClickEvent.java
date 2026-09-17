package com.example.shortlink.events;

import java.time.Instant;

public record ClickEvent(String code, String clickedAt, String correlationId) {

    public static ClickEvent now(String code) {
        return now(code, null);
    }

    public static ClickEvent now(String code, String correlationId) {
        return new ClickEvent(code, Instant.now().toString(), correlationId);
    }

    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\"code\":\"").append(code)
                .append("\",\"clickedAt\":\"").append(clickedAt).append("\"");
        if (correlationId != null && !correlationId.isBlank()) {
            json.append(",\"correlationId\":\"").append(correlationId).append("\"");
        }
        json.append("}");
        return json.toString();
    }

    public static ClickEvent fromJson(String json) {
        return new ClickEvent(
                jsonField(json, "code"),
                jsonField(json, "clickedAt"),
                optionalJsonField(json, "correlationId"));
    }

    private static String optionalJsonField(String json, String name) {
        try {
            return jsonField(json, name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String jsonField(String json, String name) {
        String key = "\"" + name + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) {
            throw new IllegalArgumentException("Missing field: " + name);
        }
        start += key.length();
        int end = json.indexOf('"', start);
        if (end < 0) {
            throw new IllegalArgumentException("Unterminated field: " + name);
        }
        return json.substring(start, end);
    }
}
