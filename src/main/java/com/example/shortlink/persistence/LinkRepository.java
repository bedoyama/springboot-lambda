package com.example.shortlink.persistence;

import com.example.shortlink.domain.Link;

import java.util.Optional;

public interface LinkRepository {

    boolean create(Link link);

    Optional<Link> findByCode(String code);

    Optional<Link> incrementClicks(String code);
}
