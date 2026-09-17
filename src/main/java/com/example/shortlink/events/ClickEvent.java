package com.example.shortlink.events;

import java.time.Instant;

public record ClickEvent(String code, String clickedAt) {

    public static ClickEvent now(String code) {
        return new ClickEvent(code, Instant.now().toString());
    }

    public String toJson() {
        return "{\"code\":\"" + code + "\",\"clickedAt\":\"" + clickedAt + "\"}";
    }

    public static ClickEvent fromJson(String json) {
        return new ClickEvent(jsonField(json, "code"), jsonField(json, "clickedAt"));
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
