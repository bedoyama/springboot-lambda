package com.example.shortlink.persistence;

import com.example.shortlink.domain.Link;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Repository
public class InMemoryLinkRepository implements LinkRepository {

    private final ConcurrentHashMap<String, Link> links = new ConcurrentHashMap<>();

    @Override
    public boolean create(Link link) {
        return links.putIfAbsent(link.code(), link) == null;
    }

    @Override
    public Optional<Link> findByCode(String code) {
        return Optional.ofNullable(links.get(code));
    }

    @Override
    public Optional<Link> incrementClicks(String code) {
        AtomicReference<Link> updated = new AtomicReference<>();
        links.computeIfPresent(code, (ignored, current) -> {
            Link next = current.incrementClicks();
            updated.set(next);
            return next;
        });
        return Optional.ofNullable(updated.get());
    }
}
