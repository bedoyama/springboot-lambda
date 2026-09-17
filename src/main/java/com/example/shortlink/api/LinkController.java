package com.example.shortlink.api;

import com.example.shortlink.domain.Link;
import com.example.shortlink.domain.LinkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class LinkController {

    private final LinkService links;

    public LinkController(LinkService links) {
        this.links = links;
    }

    @PostMapping("/links")
    public ResponseEntity<CreateLinkResponse> create(@Valid @RequestBody CreateLinkRequest request) {
        Link link = links.create(request.url());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateLinkResponse(link.code(), "/r/" + link.code(), link.originalUrl()));
    }

    @GetMapping("/links/{code}")
    public LinkStatsResponse stats(@PathVariable String code) {
        Link link = links.get(code);
        return new LinkStatsResponse(link.originalUrl(), link.createdAt(), link.clickCount());
    }

    @GetMapping("/r/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        Link link = links.redirect(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(link.originalUrl()))
                .build();
    }
}
