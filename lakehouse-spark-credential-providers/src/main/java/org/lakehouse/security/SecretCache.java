package org.lakehouse.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TTL-based in-memory secret cache.
 * Prevents DDoS on secret-server APIs when thousands of Spark partitions call getSecret per second.
 */
public final class SecretCache {

    private static final long DEFAULT_TTL_MS = 5 * 60 * 1000L;

    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();
    private final long ttlMs;

    public SecretCache() {
        this(DEFAULT_TTL_MS);
    }

    public SecretCache(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    public String get(String key) {
        Entry e = cache.get(key);
        if (e != null && !e.expired()) {
            return e.value;
        }
        return null;
    }

    public void put(String key, String value) {
        cache.put(key, new Entry(value, System.nanoTime() + ttlMs * 1_000_000L));
    }

    public void invalidate(String key) {
        cache.remove(key);
    }

    public void clear() {
        cache.clear();
    }

    private static final class Entry {
        final String value;
        final long expiresAtNanos;

        Entry(String value, long expiresAtNanos) {
            this.value = value;
            this.expiresAtNanos = expiresAtNanos;
        }

        boolean expired() {
            return System.nanoTime() > expiresAtNanos;
        }
    }
}
