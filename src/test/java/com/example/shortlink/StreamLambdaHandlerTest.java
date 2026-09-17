package com.example.shortlink;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class StreamLambdaHandlerTest {

    @Test
    void healthEventReturnsUp() throws Exception {
        StreamLambdaHandler handler = new StreamLambdaHandler();
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        try (InputStream input = Files.newInputStream(Path.of("events/health.json"))) {
            handler.handleRequest(input, output, new TestLambdaContext());
        }

        String response = output.toString(StandardCharsets.UTF_8);
        assertThat((Integer) JsonPath.read(response, "$.statusCode")).isEqualTo(200);

        String body = JsonPath.read(response, "$.body");
        assertThat((String) JsonPath.read(body, "$.status")).isEqualTo("UP");
    }
}
