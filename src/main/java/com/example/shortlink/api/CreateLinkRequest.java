package com.example.shortlink.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateLinkRequest(
        @NotBlank(message = "url is required")
        @Pattern(regexp = "https?://\\S+", message = "url must start with http:// or https://")
        String url
) {
}
