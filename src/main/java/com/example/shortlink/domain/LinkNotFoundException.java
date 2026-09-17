package com.example.shortlink.domain;

public class LinkNotFoundException extends RuntimeException {

    public LinkNotFoundException(String code) {
        super("Link not found: " + code);
    }
}
