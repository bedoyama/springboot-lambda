package com.example.shortlink.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LinkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createRedirectAndStats() throws Exception {
        MvcResult created = mockMvc.perform(post("/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com/article\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(matchesPattern("[0-9A-Za-z]{7}")))
                .andExpect(jsonPath("$.shortUrl").value(matchesPattern("/r/[0-9A-Za-z]{7}")))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/article"))
                .andExpect(header().string(CorrelationIdFilter.HEADER, not(emptyString())))
                .andReturn();

        String code = JsonPath.read(created.getResponse().getContentAsString(), "$.code");

        mockMvc.perform(get("/links/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clickCount").value(0));

        mockMvc.perform(get("/r/" + code).header(CorrelationIdFilter.HEADER, "test-correlation"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/article"))
                .andExpect(header().string(CorrelationIdFilter.HEADER, "test-correlation"));

        mockMvc.perform(get("/links/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/article"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.clickCount").value(1));
    }

    @Test
    void createRejectsBlankAndNonHttpUrls() throws Exception {
        mockMvc.perform(post("/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"ftp://example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownCodeReturns404() throws Exception {
        mockMvc.perform(get("/links/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Link not found: missing"));

        mockMvc.perform(get("/r/missing"))
                .andExpect(status().isNotFound());
    }
}
