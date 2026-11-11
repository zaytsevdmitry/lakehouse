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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityMetricsTest {

    @AfterEach
    void resetCounters() {
        SecurityMetrics.reset();
    }

    @Test
    void countersAccumulateValues() {
        SecurityMetrics.increment(SecurityMetrics.VAULT_AUTH_TOTAL);
        SecurityMetrics.increment(SecurityMetrics.VAULT_AUTH_TOTAL);
        SecurityMetrics.increment(SecurityMetrics.VAULT_AUTH_TOTAL);

        assertEquals(3, SecurityMetrics.value(SecurityMetrics.VAULT_AUTH_TOTAL));
        assertEquals(0, SecurityMetrics.value("never_incremented_total"));
    }

    @Test
    void labelledCountersAreIndependent() {
        SecurityMetrics.increment(SecurityMetrics.VAULT_HTTP_REQUESTS_TOTAL,
                SecurityMetrics.LABEL_STATUS, "200");
        SecurityMetrics.increment(SecurityMetrics.VAULT_HTTP_REQUESTS_TOTAL,
                SecurityMetrics.LABEL_STATUS, "403");
        SecurityMetrics.increment(SecurityMetrics.VAULT_HTTP_REQUESTS_TOTAL,
                SecurityMetrics.LABEL_STATUS, "200");

        assertEquals(2, SecurityMetrics.value(
                SecurityMetrics.VAULT_HTTP_REQUESTS_TOTAL, SecurityMetrics.LABEL_STATUS, "200"));
        assertEquals(1, SecurityMetrics.value(
                SecurityMetrics.VAULT_HTTP_REQUESTS_TOTAL, SecurityMetrics.LABEL_STATUS, "403"));
    }

    @Test
    void amountIncrementAddsDelta() {
        SecurityMetrics.increment(SecurityMetrics.CACHE_EVICTIONS_TOTAL, 7);
        SecurityMetrics.increment(SecurityMetrics.CACHE_EVICTIONS_TOTAL);
        assertEquals(8, SecurityMetrics.value(SecurityMetrics.CACHE_EVICTIONS_TOTAL));
    }

    @Test
    void scrapeRendersPrometheusTextFormat() {
        SecurityMetrics.increment(SecurityMetrics.CACHE_HITS_TOTAL);
        SecurityMetrics.increment(SecurityMetrics.VAULT_HTTP_REQUESTS_TOTAL,
                SecurityMetrics.LABEL_STATUS, "200");
        SecurityMetrics.increment(SecurityMetrics.VAULT_HTTP_REQUESTS_TOTAL,
                SecurityMetrics.LABEL_STATUS, "403");

        String scrape = SecurityMetrics.scrapePrometheus();

        assertTrue(scrape.contains("# TYPE cache_hits_total counter\n"));
        assertTrue(scrape.contains("cache_hits_total 1\n"));
        assertTrue(scrape.contains("# TYPE vault_http_requests_total counter\n"));
        assertTrue(scrape.contains("vault_http_requests_total{status=\"200\"} 1\n"));
        assertTrue(scrape.contains("vault_http_requests_total{status=\"403\"} 1\n"));
        assertFalse(scrape.contains("{status=\"200\"}{"), "labels must be rendered exactly once");
    }

    @Test
    void snapshotKeysAreRenderedMetricIds() {
        SecurityMetrics.increment(SecurityMetrics.VAULT_REQUESTS_TOTAL,
                SecurityMetrics.LABEL_OP, "read");
        Map<String, Long> snapshot = SecurityMetrics.snapshot();
        assertEquals(1L, snapshot.get("vault_requests_total{op=\"read\"}"));
    }
}
