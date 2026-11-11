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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.yarina.security.SecurityMetrics.CACHE_EVICTIONS_TOTAL;
import static org.yarina.security.SecurityMetrics.CACHE_HITS_TOTAL;
import static org.yarina.security.SecurityMetrics.CACHE_MISSES_TOTAL;

class SecretCacheTest {

    @Test
    void expiredEntryIsRemovedOnGet() throws InterruptedException {
        SecretCache cache = new SecretCache(50);
        cache.put("key", "value");

        long evictionsBefore = SecurityMetrics.value(CACHE_EVICTIONS_TOTAL);
        long missesBefore = SecurityMetrics.value(CACHE_MISSES_TOTAL);

        TimeUnit.MILLISECONDS.sleep(120);

        assertNull(cache.get("key"), "expired entry must not be served");
        // Entry is removed, not just ignored: the eviction counter moved and a fresh
        // value for the same key is served afterwards.
        assertTrue(SecurityMetrics.value(CACHE_EVICTIONS_TOTAL) > evictionsBefore,
                "expired entry must be evicted");
        assertTrue(SecurityMetrics.value(CACHE_MISSES_TOTAL) > missesBefore);
        cache.put("key", "value2");
        assertEquals("value2", cache.get("key"));
    }

    @Test
    void computeIfAbsentCachesResult() {
        SecretCache cache = new SecretCache();
        AtomicInteger loads = new AtomicInteger();
        assertEquals("a", cache.computeIfAbsent("k", k -> {
            loads.incrementAndGet();
            return "a";
        }));
        assertEquals("a", cache.computeIfAbsent("k", k -> {
            loads.incrementAndGet();
            return "b";
        }));
        assertEquals(1, loads.get(), "second call must be served from the cache");
        assertEquals("a", cache.get("k"));
    }

    @Test
    void computeIfAbsentSingleFlightUnderConcurrency() throws Exception {
        SecretCache cache = new SecretCache();
        AtomicInteger loads = new AtomicInteger();
        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return cache.computeIfAbsent("thundering-herd-key", k -> {
                        loads.incrementAndGet();
                        // Emulate a slow HTTP request.
                        try {
                            TimeUnit.MILLISECONDS.sleep(200);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("interrupted", e);
                        }
                        return "secret";
                    });
                }));
            }
            start.countDown();
            for (Future<String> future : futures) {
                assertEquals("secret", future.get(20, TimeUnit.SECONDS));
            }
            assertEquals(1, loads.get(), "a concurrent miss must cause exactly one load, not " + threads);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void computeIfAbsentDoesNotBlockUnrelatedKeys() throws Exception {
        SecretCache cache = new SecretCache();
        CountDownLatch slowLoadStarted = new CountDownLatch(1);
        CountDownLatch slowLoadFinished = new CountDownLatch(1);

        Thread slowLoader = new Thread(() -> {
            try {
                cache.computeIfAbsent("slow-key", k -> {
                    slowLoadStarted.countDown();
                    try {
                        slowLoadFinished.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return "slow";
                });
            } finally {
                slowLoadFinished.countDown();
            }
        });
        slowLoader.start();
        assertTrue(slowLoadStarted.await(5, TimeUnit.SECONDS), "slow loader must have started");

        // While the slow key is being loaded, another key must resolve without waiting.
        assertEquals("fast", cache.computeIfAbsent("fast-key", k -> "fast"));

        slowLoadFinished.countDown();
        slowLoader.join(5000);
        assertEquals("slow", cache.get("slow-key"));
    }

    @Test
    void computeIfAbsentDoesNotCacheErrors() {
        SecretCache cache = new SecretCache();
        AtomicInteger loads = new AtomicInteger();
        assertThrows(SecurityException.class, () -> cache.computeIfAbsent("failing-key", k -> {
            loads.incrementAndGet();
            throw new SecurityException("vault unreachable");
        }));
        assertEquals(1, loads.get());
        // The failure was not cached: the next attempt loads again and may succeed.
        assertEquals("recovered", cache.computeIfAbsent("failing-key", k -> {
            loads.incrementAndGet();
            return "recovered";
        }));
        assertEquals(2, loads.get());
    }

    @Test
    void computeIfAbsentPropagatesErrorToAllWaiters() throws Exception {
        SecretCache cache = new SecretCache();
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<String>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return cache.computeIfAbsent("failing-concurrent-key", k -> {
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        throw new SecurityException("load failed");
                    });
                }));
            }
            start.countDown();
            int failures = 0;
            for (Future<String> future : futures) {
                try {
                    future.get(20, TimeUnit.SECONDS);
                } catch (ExecutionException e) {
                    assertTrue(e.getCause() instanceof SecurityException,
                            "waiters must receive the loader exception");
                    failures++;
                }
            }
            assertEquals(threads, failures, "every waiter must observe the failure");
            assertNull(cache.get("failing-concurrent-key"), "errors must not be cached");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void invalidateRemovesOnlyOwnKey() {
        SecretCache cache = new SecretCache();
        cache.put("a", "1");
        cache.put("b", "2");
        cache.invalidate("a");
        assertNull(cache.get("a"));
        assertEquals("2", cache.get("b"), "invalidate must not touch neighbouring keys");
    }

    @Test
    void clearRemovesEverything() {
        SecretCache cache = new SecretCache();
        cache.put("a", "1");
        cache.put("b", "2");
        cache.clear();
        assertNull(cache.get("a"));
        assertNull(cache.get("b"));
    }

    @Test
    void hitAndMissCountersAreMaintained() {
        SecretCache cache = new SecretCache();
        long hitsBefore = SecurityMetrics.value(CACHE_HITS_TOTAL);
        long missesBefore = SecurityMetrics.value(CACHE_MISSES_TOTAL);

        cache.put("metric-key", "v");
        assertEquals("v", cache.get("metric-key"));     // hit
        assertNull(cache.get("metric-key-absent"));      // miss

        assertEquals(hitsBefore + 1, SecurityMetrics.value(CACHE_HITS_TOTAL));
        assertEquals(missesBefore + 1, SecurityMetrics.value(CACHE_MISSES_TOTAL));
    }
}
