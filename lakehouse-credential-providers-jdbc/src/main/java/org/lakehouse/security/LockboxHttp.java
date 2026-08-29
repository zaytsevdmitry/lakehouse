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
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight HTTP client for Yandex Cloud Lockbox v1 API.
 * Uses Instance Metadata Service for automatic IAM-token retrieval.
 */
public final class LockboxHttp {

    private static final Logger LOG = LoggerFactory.getLogger(LockboxHttp.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);
    private static final String METADATA_URL =
            "http://169.254.169.254/computeMetadata/v1/instance/service-accounts/default/token";
    private static final String LOCKBOX_API =
            "https://payload.lockbox.api.cloud.yandex.net/payload/v1/secrets";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpClient client;

    public LockboxHttp() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    /**
     * Retrieve a key from a Yandex Lockbox secret.
     *
     * @param secretId  Lockbox secret ID
     * @param secretKey key inside the secret payload
     * @param versionId optional version ID; pass {@code null} or {@code "latest"} for latest
     * @return the secret value
     */
    public String getSecret(String secretId, String secretKey, String versionId) {
        String iamToken = getIamToken();
        String version = (versionId == null || versionId.isBlank()) ? "latest" : versionId;
        String url = LOCKBOX_API + "/" + secretId + "/version/" + version;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + iamToken)
                .timeout(READ_TIMEOUT)
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.error("Lockbox returned HTTP {} for secret [redacted]", response.statusCode());
                throw new SecurityException("Lockbox access denied, HTTP " + response.statusCode());
            }
            JsonNode root = MAPPER.readTree(response.body());
            JsonNode payload = root.path("payload");
            if (!payload.isArray()) {
                throw new SecurityException("Lockbox response missing 'payload' array");
            }
            for (JsonNode entry : payload) {
                if (secretKey.equals(entry.path("key").asText())) {
                    String val = entry.path("value").asText();
                    if (val == null || val.isEmpty()) {
                        throw new SecurityException("Lockbox secret value is empty for key");
                    }
                    return val;
                }
            }
            throw new SecurityException("Lockbox key not found in secret payload");
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to read Lockbox secret: {}", e.getMessage());
            throw new SecurityException("Lockbox secret retrieval failed: " + e.getMessage(), e);
        }
    }

    /**
     * Retrieve all key-value pairs from a Yandex Lockbox secret.
     */
    public Map<String, String> getAll(String secretId, String versionId) {
        String iamToken = getIamToken();
        String version = (versionId == null || versionId.isBlank()) ? "latest" : versionId;
        String url = LOCKBOX_API + "/" + secretId + "/version/" + version;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + iamToken)
                .timeout(READ_TIMEOUT)
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                LOG.error("Lockbox returned HTTP {}", response.statusCode());
                throw new SecurityException("Lockbox access denied, HTTP " + response.statusCode());
            }
            JsonNode root = MAPPER.readTree(response.body());
            JsonNode payload = root.path("payload");
            Map<String, String> result = new HashMap<>();
            if (payload.isArray()) {
                for (JsonNode entry : payload) {
                    String k = entry.path("key").asText();
                    String v = entry.path("value").asText();
                    if (k != null && !k.isEmpty()) {
                        result.put(k, v);
                    }
                }
            }
            return result;
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Failed to read Lockbox secret: {}", e.getMessage());
            throw new SecurityException("Lockbox secret retrieval failed: " + e.getMessage(), e);
        }
    }

    /* ------------------------------------------------------------------ */

    private String getIamToken() {
        String akPath = System.getenv("YC_AUTH_KEY_PATH");
        if (akPath != null && !akPath.isEmpty()) {
            return getIamTokenViaAuthorizedKey(akPath);
        }
        return getIamTokenViaMetadata();
    }

    private String getIamTokenViaMetadata() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(METADATA_URL))
                .header("Metadata-Flavor", "Google")
                .timeout(READ_TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = MAPPER.readTree(response.body());
                return root.path("iamToken").asText();
            }
            LOG.error("Metadata service returned HTTP {}", response.statusCode());
            throw new SecurityException(
                    "Cannot obtain IAM token from metadata service, HTTP " + response.statusCode());
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Metadata service request failed: {}", e.getMessage());
            throw new SecurityException("Metadata service unreachable: " + e.getMessage(), e);
        }
    }

    private String getIamTokenViaAuthorizedKey(String keyFilePath) {
        try {
            String keyContent = new String(java.nio.file.Files.readAllBytes(java.nio.file.Path.of(keyFilePath)));
            com.fasterxml.jackson.databind.JsonNode keyNode = MAPPER.readTree(keyContent);
            String serviceAccountId = keyNode.path("service_account_id").asText(null);
            String oauthToken = keyNode.path("oauth_token").asText(null);

            if (oauthToken != null && !oauthToken.isEmpty()) {
                String body = MAPPER.writeValueAsString(Map.of("yandexPassportOauthToken", oauthToken));
                return requestIamToken(body);
            }

            if (serviceAccountId == null || serviceAccountId.isEmpty()) {
                throw new SecurityException("Authorized key file missing service_account_id or oauth_token");
            }
            throw new SecurityException(
                    "Service-account key auth requires the Yandex Cloud SDK. " +
                            "Use an OAuth token in the authorized key file, or rely on Instance Metadata.");
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("IAM token request via authorized key failed: {}", e.getMessage());
            throw new SecurityException("IAM token request failed: " + e.getMessage(), e);
        }
    }

    private String requestIamToken(String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://iam.api.cloud.yandex.net/iam/v1/tokens"))
                .header("Content-Type", "application/json")
                .timeout(READ_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            JsonNode root = MAPPER.readTree(response.body());
            return root.path("iamToken").asText();
        }
        LOG.error("IAM token request returned HTTP {}", response.statusCode());
        throw new SecurityException("Cannot obtain IAM token, HTTP " + response.statusCode());
    }
}
