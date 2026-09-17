package com.example.shortlink.domain;

import org.crac.Context;
import org.crac.Core;
import org.crac.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Codes are generated per request, never at class init. After SnapStart restore,
 * {@link SecureRandom} is reseeded so restored environments do not share the
 * snapshot's RNG state.
 */
@Component
public class ShortCodeGenerator implements Resource {

    private static final Logger log = LoggerFactory.getLogger(ShortCodeGenerator.class);
    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int LENGTH = 7;

    private volatile SecureRandom random = new SecureRandom();

    public ShortCodeGenerator() {
        Core.getGlobalContext().register(this);
    }

    public String next() {
        char[] code = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            code[i] = ALPHABET[random.nextInt(ALPHABET.length)];
        }
        return new String(code);
    }

    @Override
    public void beforeCheckpoint(Context<? extends Resource> context) {
        // nothing to close
    }

    @Override
    public void afterRestore(Context<? extends Resource> context) {
        random = new SecureRandom();
        log.info("SnapStart restore: reseeded SecureRandom");
    }
}
