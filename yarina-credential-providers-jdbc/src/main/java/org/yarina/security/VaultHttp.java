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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Lightweight HTTP client for OpenBao / HashiCorp Vault KV v2 API.
 * Supports Token auth (VAULT_TOKEN env), Kubernetes auth (service-account token)
 * and explicit tokens (for multi-cloud setups with several OpenBao instances).
 *
 * <p><b>HttpClient is shared.</b> A single JVM-wide {@link HttpClient} backs every
 * {@code VaultHttp} instance (including the Kubernetes login) — {@code HttpClient} is
 * thread-safe and reuses connections, so clients no longer multiply sockets.
 *
 * <p><b>Token refresh.</b> When a read is rejected with HTTP 403 and the token was
 * obtained via Kubernetes auth, the client re-authenticates (K8s login) and repeats
 * the request <b>once</b> — there is no retry storm. Concurrent 403s trigger a single
 * re-login. Tokens taken from {@code VAULT_TOKEN} or passed explicitly are static and
 * are not refreshed.
 *
 * <p><b>Batch reads.</b> {@link #getSecretPayload(String)} performs one HTTP request and
 * returns the whole payload of the secret, so multi-key secrets (e.g. an S3 key pair)
 * need a single call instead of one request per key.
 *
 * <p>Option keys (the {@code options} map):
 * <ul>
 *   <li>{@value #OPT_VAULT_ROLE} — Kubernetes auth role, default {@code yarina}</li>
 *   <li>{@value #OPT_K8S_AUTH_PATH} — Kubernetes auth mount path, default {@code kubernetes}</li>
 *   <li>{@value #OPT_K8S_TOKEN_PATH} — service-account JWT path, default
 *       {@code /var/run/secrets/kubernetes.io/serviceaccount/token}</li>
 * </ul>
 *
 * <p>Token resolution priority: explicit token (constructor argument) &gt;
 * {@code VAULT_TOKEN} environment variable &gt; Kubernetes auth.
 */
public final class VaultHttp {

    private static final Logger LOG = LoggerFactory.getLogger(VaultHttp.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    private static final String K8S_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    private static final String DEFAULT_K8S_AUTH_PATH = "kubernetes";
    private static final String DEFAULT_VAULT_ROLE = "yarina";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Option key: Kubernetes auth role. */
    public static final String OPT_VAULT_ROLE = "vault-role";
    /** Option key: Kubernetes auth mount path. */
    public static final String OPT_K8S_AUTH_PATH = "vault-k8s-auth-path";
    /** Option key: path to the Kubernetes service-account JWT. */
    public static final String OPT_K8S_TOKEN_PATH = "vault-k8s-token-path";

    /** JVM-wide shared HttpClient: thread-safe and reusable for all instances and logins. */
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    private final String vaultUrl;
    private final Map<String, String> options;
    /** Current Vault token; replaced by a 403-triggered re-auth. */
    private volatile String token;
    /** True when the token came from Kubernetes auth and can be refreshed by a re-login. */
    private final boolean tokenRenewable;
    /** Serialises re-auth so a mass token expiry causes one login, not one per thread. */
    private final Object authLock = new Object();

    public VaultHttp(String vaultUrl, Map<String, String> options) {
        this(vaultUrl, options, null);
    }

    /**
     * @param vaultUrl      Vault HTTP API base URL, e.g. {@code http://vault:8200}
     * @param options       client options ({@value #OPT_VAULT_ROLE}, {@value #OPT_K8S_AUTH_PATH},
     *                      {@value #OPT_K8S_TOKEN_PATH}); may be {@code null}
     * @param explicitToken token to use as-is; takes priority over {@code VAULT_TOKEN} and
     *                      Kubernetes auth. Needed for multi-cloud setups where different
     *                      OpenBao instances are authenticated with different tokens.
     *                      May be {@code null} — then the usual resolution applies.
     */
    public VaultHttp(String vaultUrl, Map<String, String> options, String explicitToken) {
        if (vaultUrl == null || vaultUrl.isBlank()) {
            throw new IllegalArgumentException("vaultUrl must not be null or blank");
        }
        String trimmed = vaultUrl.strip();
        this.vaultUrl = trimmed.endsWith("/")
                ? trimmed.substring(0, trimmed.length() - 1)
                : trimmed;
        this.options = safeCopy(options);

        if (explicitToken != null && !explicitToken.isBlank()) {
            this.token = explicitToken;
            this.tokenRenewable = false;
        } else {
            String envToken = System.getenv("VAULT_TOKEN");
            if (envToken != null && !envToken.isEmpty()) {
                LOG.info("Using Vault token from VAULT_TOKEN environment variable");
                this.token = envToken;
                this.tokenRenewable = false;
            } else {
                String k8sJwt = readK8sToken();
                if (k8sJwt == null) {
                    throw new SecurityException(
                            "No Vault token available: set VAULT_TOKEN env or run inside Kubernetes with a service account");
                }
                this.token = authenticateKubernetes(k8sJwt);
                this.tokenRenewable = true;
            }
        }
    }

    /**
     * Retrieve a secret value from KV v2.
     *
     * @param secretPath path inside KV engine, e.g. "secret/data/myapp/db"
     * @param secretKey  key inside the secret, e.g. "password"
     * @return the secret value
     */
    public String getSecret(String secretPath, String secretKey) {
        requireNonBlank("secretPath", secretPath);
        requireNonBlank("secretKey", secretKey);
        String value = getSecretPayload(secretPath).get(secretKey);
        if (value == null) {
            throw new SecurityException("Secret key not found in Vault response");
        }
        return value;
    }

    /**
     * Retrieve the whole payload of a secret with a single HTTP request.
     *
     * @param secretPath path inside KV engine, e.g. "secret/data/myapp/db"
     * @return all scalar key-value pairs of the secret (KV v2: {@code response.data.data},
     *         KV v1: {@code response.data}); never {@code null}, possibly empty
     */
    public Map<String, String> getSecretPayload(String secretPath) {
        requireNonBlank("secretPath", secretPath);

        String attemptToken = token;
        HttpResponse<String> response = readSecret(secretPath, attemptToken);
        if (response.statusCode() == 403 && tokenRenewable) {
            // Token expired: re-authenticate (single-flight) and retry exactly once.
            String refreshedToken = refreshToken(attemptToken);
            response = readSecret(secretPath, refreshedToken);
        }
        if (response.statusCode() != 200) {
            LOG.error("Vault returned HTTP {} for path [redacted]", response.statusCode());
            throw new SecurityException("Vault access denied, HTTP " + response.statusCode());
        }
        return parsePayload(response.body());
    }

    /* ------------------------------------------------------------------ */

    private HttpResponse<String> readSecret(String secretPath, String authToken) {
        String url = vaultUrl + "/v1/" + secretPath;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Vault-Token", authToken)
                .timeout(READ_TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            SecurityMetrics.increment(SecurityMetrics.VAULT_HTTP_REQUESTS_TOTAL,
                    SecurityMetrics.LABEL_STATUS, Integer.toString(response.statusCode()));
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Interrupted while reading secret from Vault");
            throw new SecurityException("Vault secret retrieval failed: interrupted", e);
        } catch (IOException | RuntimeException e) {
            LOG.error("Failed to read secret from Vault: {}", e.getMessage());
            throw new SecurityException("Vault secret retrieval failed: " + e.getMessage(), e);
        }
    }

    private static Map<String, String> parsePayload(String body) {
        try {
            JsonNode root = MAPPER.readTree(body);
            JsonNode dataNode = root.has("data") ? root.get("data") : null;
            if (dataNode == null || dataNode.isNull()) {
                throw new SecurityException("Vault response missing 'data' field");
            }
            // KV v2: response.data.data.{key}  |  KV v1: response.data.{key}
            JsonNode inner = dataNode.has("data") && dataNode.get("data").isObject()
                    ? dataNode.get("data")
                    : dataNode;
            Map<String, String> payload = new LinkedHashMap<>();
            if (inner.isObject()) {
                inner.fields().forEachRemaining(field -> {
                    JsonNode valueNode = field.getValue();
                    if (valueNode != null && valueNode.isValueNode()) {
                        payload.put(field.getKey(), valueNode.asText());
                    }
                });
            }
            return Collections.unmodifiableMap(payload);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to parse Vault response for path [redacted]: {}", e.getMessage());
            throw new SecurityException("Vault secret retrieval failed: " + e.getMessage(), e);
        }
    }

    /**
     * Refresh the token when Vault rejects {@code staleToken} with HTTP 403.
     * Single-flight: when several threads observe the expiry simultaneously only one
     * re-login happens; the others reuse the fresh token.
     */
    private String refreshToken(String staleToken) {
        synchronized (authLock) {
            String current = token;
            if (!Objects.equals(current, staleToken)) {
                // Another thread already re-authenticated — reuse its token.
                return current;
            }
            LOG.info("Vault token was rejected (HTTP 403); re-authenticating");
            String k8sJwt = readK8sToken();
            if (k8sJwt == null) {
                throw new SecurityException(
                        "Vault token refresh failed: Kubernetes service-account token is not available");
            }
            String freshToken = authenticateKubernetes(k8sJwt);
            token = freshToken;
            SecurityMetrics.increment(SecurityMetrics.VAULT_TOKEN_REFRESH_TOTAL);
            return freshToken;
        }
    }

    private String readK8sToken() {
        String tokenPath = options.getOrDefault(OPT_K8S_TOKEN_PATH, K8S_TOKEN_PATH);
        try {
            return Files.readString(Path.of(tokenPath)).trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String authenticateKubernetes(String k8sJwt) {
        String role = options.getOrDefault(OPT_VAULT_ROLE, DEFAULT_VAULT_ROLE);
        String mountPath = options.getOrDefault(OPT_K8S_AUTH_PATH, DEFAULT_K8S_AUTH_PATH);

        String url = vaultUrl + "/v1/auth/" + mountPath + "/login";
        String body;
        try {
            body = MAPPER.writeValueAsString(Map.of("role", role, "jwt", k8sJwt));
        } catch (Exception e) {
            throw new SecurityException("Failed to serialise k8s auth payload", e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(READ_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            SecurityMetrics.increment(SecurityMetrics.VAULT_AUTH_TOTAL);
            if (response.statusCode() != 200) {
                LOG.error("Vault k8s auth returned HTTP {}", response.statusCode());
                throw new SecurityException("Vault k8s auth failed, HTTP " + response.statusCode());
            }
            JsonNode root = MAPPER.readTree(response.body());
            String clientToken = root.path("auth").path("client_token").asText(null);
            if (clientToken == null || clientToken.isEmpty()) {
                throw new SecurityException("Vault k8s auth response missing client_token");
            }
            LOG.info("Vault k8s auth successful");
            return clientToken;
        } catch (SecurityException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SecurityException("Vault k8s auth failed: interrupted", e);
        } catch (Exception e) {
            LOG.error("Vault k8s auth failed: {}", e.getMessage());
            throw new SecurityException("Vault k8s auth failed: " + e.getMessage(), e);
        }
    }

    private static Map<String, String> safeCopy(Map<String, String> options) {
        if (options == null || options.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new HashMap<>();
        for (Map.Entry<String, String> entry : options.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                copy.put(entry.getKey(), entry.getValue());
            }
        }
        return Collections.unmodifiableMap(copy);
    }

    private static void requireNonBlank(String name, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be null or blank");
        }
    }
}
