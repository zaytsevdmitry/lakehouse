/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
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
package org.lakehouse.security;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TTL-based in-memory secret cache.
 * Prevents DDoS on secret-server APIs when many threads/partitions call getSecret concurrently.
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
