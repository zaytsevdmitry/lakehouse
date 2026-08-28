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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

/**
 * Lightweight HTTP client for OpenBao / HashiCorp Vault KV v2 API.
 * Supports Token auth (VAULT_TOKEN env) and Kubernetes auth (service-account token).
 */
public final class VaultHttp {

    private static final Logger LOG = LoggerFactory.getLogger(VaultHttp.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    private static final String K8S_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient client;
    private final String vaultUrl;
    private final String token;

    public VaultHttp(String vaultUrl, Map<String, String> options) {
        this.vaultUrl = vaultUrl;
        this.client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.token = resolveToken(vaultUrl, options);
    }

    /**
     * Retrieve a secret value from KV v2.
     *
     * @param secretPath path inside KV engine, e.g. "secret/data/myapp/db"
     * @param secretKey  key inside the secret, e.g. "password"
     * @return the secret value
     */
    public String getSecret(String secretPath, String secretKey) {
        String url = vaultUrl + "/v1/" + secretPath;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-Vault-Token", token)
                .timeout(READ_TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.error("Vault returned HTTP {} for path [redacted]", response.statusCode());
                throw new SecurityException("Vault access denied, HTTP " + response.statusCode());
            }
            JsonNode root = MAPPER.readTree(response.body());
            JsonNode dataNode = root.has("data") ? root.get("data") : null;
            if (dataNode == null) {
                throw new SecurityException("Vault response missing 'data' field");
            }
            // KV v2: response.data.data.{key}  |  KV v1: response.data.{key}
            JsonNode inner = dataNode.has("data") ? dataNode.get("data") : dataNode;
            JsonNode valueNode = inner.get(secretKey);
            if (valueNode == null || valueNode.isNull()) {
                throw new SecurityException("Secret key not found in Vault response");
            }
            return valueNode.asText();
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to read secret from Vault: {}", e.getMessage());
            throw new SecurityException("Vault secret retrieval failed: " + e.getMessage(), e);
        }
    }

    /* ------------------------------------------------------------------ */

    private static String resolveToken(String vaultUrl, Map<String, String> options) {
        String envToken = System.getenv("VAULT_TOKEN");
        if (envToken != null && !envToken.isEmpty()) {
            LOG.info("Using Vault token from VAULT_TOKEN environment variable");
            return envToken;
        }
        String k8sToken = readK8sToken();
        if (k8sToken != null) {
            return authenticateKubernetes(vaultUrl, k8sToken, options);
        }
        throw new SecurityException(
                "No Vault token available: set VAULT_TOKEN env or run inside Kubernetes with a service account");
    }

    private static String readK8sToken() {
        try {
            return Files.readString(Path.of(K8S_TOKEN_PATH)).trim();
        } catch (Exception e) {
            return null;
        }
    }

    private static String authenticateKubernetes(String vaultUrl, String k8sJwt, Map<String, String> options) {
        String role = options.getOrDefault("vault-role", "lakehouse");
        String mountPath = options.getOrDefault("vault-k8s-auth-path", "kubernetes");

        String url = vaultUrl + "/v1/auth/" + mountPath + "/login";
        String body;
        try {
            body = MAPPER.writeValueAsString(Map.of("role", role, "jwt", k8sJwt));
        } catch (Exception e) {
            throw new SecurityException("Failed to serialise k8s auth payload", e);
        }

        HttpClient authClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(READ_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = authClient.send(request, HttpResponse.BodyHandlers.ofString());
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
        } catch (Exception e) {
            LOG.error("Vault k8s auth failed: {}", e.getMessage());
            throw new SecurityException("Vault k8s auth failed: " + e.getMessage(), e);
        }
    }
}
