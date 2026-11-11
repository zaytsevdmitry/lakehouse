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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.StandardMBean;
import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * JVM-wide, dependency-free counter registry for the Yarina security subsystem.
 *
 * <p>The credential-provider modules ship with <b>zero transitive runtime
 * dependencies</b> (they are dropped into Spark clusters), so Micrometer cannot be
 * added as a compile-time dependency here. Instead this class keeps simple atomic
 * counters and offers three export paths:
 *
 * <ol>
 *   <li><b>Prometheus text exposition</b> — {@link #scrapePrometheus()} renders all
 *       counters in the Prometheus text format. A platform service that already runs
 *       Spring Actuator / Micrometer (e.g. {@code yarina-task-proxy-for-spark}) can
 *       call it periodically and expose the result under its own
 *       {@code /actuator/prometheus} endpoint, or wrap the counters with
 *       {@code MeterRegistry} gauges.</li>
 *   <li><b>JMX</b> — the {@code org.yarina.security:type=SecurityMetrics} MBean exposes
 *       the same text as the {@code PrometheusText} attribute; it is always available
 *       inside a plain Spark Driver/Executor JVM (jconsole / jmx_exporter).</li>
 *   <li><b>Programmatic</b> — {@link #value(String)} / {@link #snapshot()} for tests and
 *       custom tooling.</li>
 * </ol>
 *
 * <p>Counters (all monotonic, per JVM):
 * <ul>
 *   <li>{@code cache_hits_total}, {@code cache_misses_total}, {@code cache_evictions_total}
 *       — emitted by {@link SecretCache}</li>
 *   <li>{@code vault_requests_total{op="read|auth"}} and
 *       {@code provider_instances_created_total} — emitted by the credential providers</li>
 *   <li>{@code vault_http_requests_total{status}}, {@code vault_auth_total},
 *       {@code vault_token_refresh_total} — emitted by {@link VaultHttp}</li>
 * </ul>
 *
 * <p>All methods are thread-safe. Counter values are approximate under concurrency
 * (LongAdder semantics), which is acceptable for monitoring.
 */
public final class SecurityMetrics {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityMetrics.class);

    /** SecretCache: number of cache lookups served from the cache. */
    public static final String CACHE_HITS_TOTAL = "cache_hits_total";
    /** SecretCache: number of cache lookups that missed (absent or expired entry). */
    public static final String CACHE_MISSES_TOTAL = "cache_misses_total";
    /** SecretCache: number of entries removed (TTL expiry, invalidate or clear). */
    public static final String CACHE_EVICTIONS_TOTAL = "cache_evictions_total";
    /** Credential provider: vault operations initiated by the provider itself. Label: op="read|auth". */
    public static final String VAULT_REQUESTS_TOTAL = "vault_requests_total";
    /** Credential provider: number of provider instances created in this JVM. */
    public static final String PROVIDER_INSTANCES_CREATED_TOTAL = "provider_instances_created_total";
    /** VaultHttp: HTTP read requests to Vault by response status. Label: status="200|403|...". */
    public static final String VAULT_HTTP_REQUESTS_TOTAL = "vault_http_requests_total";
    /** VaultHttp: Kubernetes logins performed. */
    public static final String VAULT_AUTH_TOTAL = "vault_auth_total";
    /** VaultHttp: token refreshes triggered by an expired token (HTTP 403). */
    public static final String VAULT_TOKEN_REFRESH_TOTAL = "vault_token_refresh_total";

    /** Optional label of {@link #VAULT_REQUESTS_TOTAL}: the initiated operation. */
    public static final String LABEL_OP = "op";
    /** Optional label of {@link #VAULT_HTTP_REQUESTS_TOTAL}: the HTTP status code. */
    public static final String LABEL_STATUS = "status";

    private static final ObjectName JMX_OBJECT_NAME = createJmxName();

    private static final ConcurrentHashMap<CounterKey, LongAdder> COUNTERS = new ConcurrentHashMap<>();

    static {
        registerJmxBean();
    }

    private SecurityMetrics() {
    }

    /**
     * Increment a counter without labels by one.
     *
     * @param name counter name, e.g. {@link #VAULT_AUTH_TOTAL}
     */
    public static void increment(String name) {
        counter(new CounterKey(requireName(name), null, null)).increment();
    }

    /**
     * Increment a counter with one label by one.
     *
     * @param name        counter name
     * @param labelName   label key, e.g. {@link #LABEL_OP}
     * @param labelValue   label value, e.g. {@code "read"}
     */
    public static void increment(String name, String labelName, String labelValue) {
        counter(new CounterKey(requireName(name), requireLabel(labelName), requireLabel(labelValue))).increment();
    }

    /**
     * Add {@code amount} to a counter without labels.
     *
     * @param name   counter name
     * @param amount amount to add (non-negative)
     */
    public static void increment(String name, long amount) {
        if (amount > 0) {
            counter(new CounterKey(requireName(name), null, null)).add(amount);
        }
    }

    /**
     * @param name counter name
     * @return current value of the unlabelled counter
     */
    public static long value(String name) {
        LongAdder adder = COUNTERS.get(new CounterKey(requireName(name), null, null));
        return adder == null ? 0L : adder.sum();
    }

    /**
     * @param name       counter name
     * @param labelName  label key
     * @param labelValue label value
     * @return current value of the labelled counter
     */
    public static long value(String name, String labelName, String labelValue) {
        LongAdder adder = COUNTERS.get(
                new CounterKey(requireName(name), requireLabel(labelName), requireLabel(labelValue)));
        return adder == null ? 0L : adder.sum();
    }

    /**
     * Snapshot of all counters, keyed by the rendered metric id
     * (e.g. {@code vault_http_requests_total{status="200"}}). The snapshot is ordered
     * by metric name for stable output.
     */
    public static Map<String, Long> snapshot() {
        Map<String, Long> result = new TreeMap<>();
        for (Map.Entry<CounterKey, LongAdder> entry : COUNTERS.entrySet()) {
            result.put(entry.getKey().toMetricId(), entry.getValue().sum());
        }
        return result;
    }

    /**
     * Render all counters in the Prometheus text exposition format
     * ({@code # TYPE <name> counter} + one sample line per counter).
     */
    public static String scrapePrometheus() {
        Map<String, Long> snapshot = snapshot();
        Map<String, StringBuilder> byName = new TreeMap<>();
        Map<String, Boolean> seen = new TreeMap<>();
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Long> entry : snapshot.entrySet()) {
            String metricId = entry.getKey();
            int brace = metricId.indexOf('{');
            String name = brace < 0 ? metricId : metricId.substring(0, brace);
            if (seen.put(name, Boolean.TRUE) == null) {
                out.append("# TYPE ").append(name).append(" counter\n");
            }
            out.append(metricId).append(' ').append(entry.getValue()).append('\n');
        }
        return out.toString();
    }

    /**
     * Drop all counters. Intended for tests and administrative tooling; production
     * code must not call it.
     */
    public static void reset() {
        COUNTERS.clear();
    }

    /* ------------------------------------------------------------------ */

    private static LongAdder counter(CounterKey key) {
        return COUNTERS.computeIfAbsent(key, k -> new LongAdder());
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("metric name must not be null or blank");
        }
        return name;
    }

    private static String requireLabel(String label) {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("metric label must not be null or blank");
        }
        return label;
    }

    private static ObjectName createJmxName() {
        try {
            return new ObjectName("org.yarina.security:type=SecurityMetrics");
        } catch (Exception e) {
            LOG.debug("SecurityMetrics JMX object name is invalid: {}", e.getMessage());
            return null;
        }
    }

    private static void registerJmxBean() {
        if (JMX_OBJECT_NAME == null) {
            return;
        }
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            if (!server.isRegistered(JMX_OBJECT_NAME)) {
                server.registerMBean(
                        new StandardMBean(new SecurityMetricsJmxView(), SecurityMetricsMBean.class),
                        JMX_OBJECT_NAME);
            }
        } catch (Exception e) {
            // Best-effort: JMX may be restricted in hardened environments.
            LOG.debug("SecurityMetrics JMX export is not available: {}", e.getMessage());
        }
    }

    /** JMX view of the registry. */
    public interface SecurityMetricsMBean {
        /** All counters in the Prometheus text exposition format. */
        String getPrometheusText();

        /** All counters keyed by rendered metric id. */
        Map<String, Long> getCounters();
    }

    private static final class SecurityMetricsJmxView implements SecurityMetricsMBean {
        @Override
        public String getPrometheusText() {
            return SecurityMetrics.scrapePrometheus();
        }

        @Override
        public Map<String, Long> getCounters() {
            return new LinkedHashMap<>(SecurityMetrics.snapshot());
        }
    }

    /** Immutable identity of one counter (name + optional single label). */
    private record CounterKey(String name, String labelName, String labelValue) {

        String toMetricId() {
            if (labelName == null) {
                return name;
            }
            return name + "{" + labelName + "=\"" + escapeLabelValue(labelValue) + "\"}";
        }

        private static String escapeLabelValue(String value) {
            return value.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }
}
