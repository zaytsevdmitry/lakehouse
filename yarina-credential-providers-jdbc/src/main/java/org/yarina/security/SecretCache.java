/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.yarina.security;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * TTL-based in-memory secret cache.
 * Prevents DDoS on secret-server APIs when many threads/partitions call getSecret concurrently.
 *
 * <p><b>Single-flight loading.</b> {@link #computeIfAbsent(String, Function)} guarantees that
 * for a given key at most one loader runs at a time: when many threads miss the cache
 * simultaneously, one thread performs the (potentially slow) loader call — typically an HTTP
 * request — while the others wait for its result. The {@link ConcurrentHashMap} bucket lock is
 * <b>never</b> held for the duration of the loader: per-key {@link CompletableFuture} slots are
 * used instead, so reads and writes of unrelated keys are never blocked by an in-flight load.
 *
 * <p>Errors are never cached: when the loader throws, the exception is propagated to every
 * waiting thread and nothing is stored.
 *
 * <p>Hit/miss/eviction counters are exported through {@link SecurityMetrics}
 * ({@code cache_hits_total}, {@code cache_misses_total}, {@code cache_evictions_total}).
 */
public final class SecretCache {

    private static final long DEFAULT_TTL_MS = 5 * 60 * 1000L;

    private final ConcurrentHashMap<String, Entry> cache = new ConcurrentHashMap<>();
    /** Per-key load slots providing single-flight semantics without bucket locks. */
    private final ConcurrentHashMap<String, CompletableFuture<String>> inFlightLoads =
            new ConcurrentHashMap<>();
    private final long ttlMs;

    public SecretCache() {
        this(DEFAULT_TTL_MS);
    }

    public SecretCache(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    /**
     * Return the cached value for the key, or {@code null} when absent.
     * Expired entries are removed best-effort so they do not accumulate in memory.
     */
    public String get(String key) {
        Objects.requireNonNull(key, "key");
        Entry e = cache.get(key);
        if (e == null) {
            SecurityMetrics.increment(SecurityMetrics.CACHE_MISSES_TOTAL);
            return null;
        }
        if (e.expired()) {
            // Best-effort removal: only removes this exact entry, so a concurrent
            // put() of a fresh value is never dropped.
            if (cache.remove(key, e)) {
                SecurityMetrics.increment(SecurityMetrics.CACHE_EVICTIONS_TOTAL);
            }
            SecurityMetrics.increment(SecurityMetrics.CACHE_MISSES_TOTAL);
            return null;
        }
        SecurityMetrics.increment(SecurityMetrics.CACHE_HITS_TOTAL);
        return e.value;
    }

    public void put(String key, String value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        cache.put(key, new Entry(value, System.nanoTime() + ttlMs * 1_000_000L));
    }

    /**
     * Return the cached value for the key, loading it with {@code loader} on a miss.
     *
     * <p>Single-flight: when several threads miss the same key at once, the loader runs
     * exactly once and every thread receives its result (or its exception). The loader runs
     * <b>outside</b> any {@link ConcurrentHashMap} bucket lock — it may block on an HTTP
     * request without freezing unrelated cache operations. The loader must not call back
     * into this cache for the same key.
     *
     * @param key    cache key
     * @param loader value loader; may throw {@link RuntimeException}
     * @return the cached or freshly loaded value; {@code null} only when the loader
     *         itself returned {@code null} (a {@code null} result is not cached)
     */
    public String computeIfAbsent(String key, Function<String, String> loader) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(loader, "loader");
        String value = get(key);
        if (value != null) {
            return value;
        }

        CompletableFuture<String> slot = new CompletableFuture<>();
        CompletableFuture<String> inFlight = inFlightLoads.putIfAbsent(key, slot);
        if (inFlight != null) {
            // Another thread is already loading this key — wait for its result.
            return awaitLoad(inFlight);
        }

        // This thread owns the load slot for the key. A concurrent loader may have
        // completed between our miss and winning the slot — re-check without
        // counting a second miss.
        try {
            value = peek(key);
            if (value == null) {
                value = loader.apply(key);
                if (value != null) {
                    put(key, value);
                }
            }
            slot.complete(value);
            return value;
        } catch (Throwable t) {
            slot.completeExceptionally(t);
            if (t instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (t instanceof Error error) {
                throw error;
            }
            // Function#apply cannot throw checked exceptions — unreachable in practice.
            throw new SecurityException("Secret load failed: " + t.getMessage(), t);
        } finally {
            inFlightLoads.remove(key, slot);
        }
    }

    /** Drop the cached value for one key; other keys are unaffected. */
    public void invalidate(String key) {
        Objects.requireNonNull(key, "key");
        if (cache.remove(key) != null) {
            SecurityMetrics.increment(SecurityMetrics.CACHE_EVICTIONS_TOTAL);
        }
    }

    /** Drop all cached values and in-flight load slots. */
    public void clear() {
        int evicted = cache.size();
        cache.clear();
        inFlightLoads.clear();
        SecurityMetrics.increment(SecurityMetrics.CACHE_EVICTIONS_TOTAL, evicted);
    }

    /* ------------------------------------------------------------------ */

    private String peek(String key) {
        Entry e = cache.get(key);
        return e != null && !e.expired() ? e.value : null;
    }

    private static String awaitLoad(CompletableFuture<String> inFlight) {
        try {
            return inFlight.join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new SecurityException("Secret load failed: " + cause.getMessage(), cause);
        }
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
