package com.example.shortlink.domain;

import com.example.shortlink.persistence.LinkRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class LinkService {

    private static final int MAX_CODE_ATTEMPTS = 8;

    private final LinkRepository links;
    private final ShortCodeGenerator codes;

    public LinkService(LinkRepository links, ShortCodeGenerator codes) {
        this.links = links;
        this.codes = codes;
    }

    public Link create(String url) {
        for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
            Link link = new Link(codes.next(), url, Instant.now(), 0);
            if (links.create(link)) {
                return link;
            }
        }
        throw new IllegalStateException("Could not allocate a unique short code");
    }

    public Link get(String code) {
        return links.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));
    }

    public Link redirect(String code) {
        return links.incrementClicks(code).orElseThrow(() -> new LinkNotFoundException(code));
    }
}
