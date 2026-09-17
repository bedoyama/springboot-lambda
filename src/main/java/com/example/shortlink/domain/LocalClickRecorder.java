package com.example.shortlink.domain;

import com.example.shortlink.persistence.LinkRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalClickRecorder implements ClickRecorder {

    private final LinkRepository links;

    public LocalClickRecorder(LinkRepository links) {
        this.links = links;
    }

    @Override
    public void record(String code) {
        links.incrementClicks(code);
    }
}
