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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VaultHttpTest {

    @TempDir
    static Path tempDir;

    private static String jwtFile;

    @BeforeAll
    static void setUp() throws IOException {
        // The Kubernetes-auth tests require that VAULT_TOKEN does not shadow the K8s path.
        String envToken = System.getenv("VAULT_TOKEN");
        Assumptions.assumeTrue(envToken == null || envToken.isBlank(),
                "VAULT_TOKEN is set in the environment; Kubernetes-auth tests are skipped");

        jwtFile = tempDir.resolve("sa-token").toString();
        Files.writeString(Path.of(jwtFile), "test-service-account-jwt", StandardCharsets.UTF_8);
    }

    private static Map<String, String> k8sOptions() {
        return Map.of(VaultHttp.OPT_K8S_TOKEN_PATH, jwtFile);
    }

    @Test
    void getSecretPayloadFetchesWholePayloadWithOneRequest() throws Exception {
        try (VaultStubServer vault = VaultStubServer.start()) {
            vault.setPayload(Map.of("password", "db-password", "user", "db-user"));
            VaultHttp http = new VaultHttp(vault.url(), k8sOptions());

            Map<String, String> payload = http.getSecretPayload("secret/data/db");

            assertEquals("db-password", payload.get("password"));
            assertEquals("db-user", payload.get("user"));
            assertEquals(1, vault.reads(), "the whole payload must be one HTTP request");
            assertEquals(1, vault.logins());
        }
    }

    @Test
    void getSecretReadsOneKeyWithOneRequest() throws Exception {
        try (VaultStubServer vault = VaultStubServer.start()) {
            vault.setPayload(Map.of("password", "db-password", "user", "db-user"));
            VaultHttp http = new VaultHttp(vault.url(), k8sOptions());

            assertEquals("db-password", http.getSecret("secret/data/db", "password"));
            assertEquals(1, vault.reads(), "getSecret must not issue a second request");
        }
    }

    @Test
    void getSecretThrowsWhenKeyIsMissing() throws Exception {
        try (VaultStubServer vault = VaultStubServer.start()) {
            vault.setPayload(Map.of("password", "db-password"));
            VaultHttp http = new VaultHttp(vault.url(), k8sOptions());

            SecurityException e = assertThrows(SecurityException.class,
                    () -> http.getSecret("secret/data/db", "absent-key"));
            assertTrue(e.getMessage().contains("Secret key not found"));
        }
    }

    @Test
    void non200FailsWithoutRetry() throws Exception {
        try (VaultStubServer vault = VaultStubServer.start()) {
            vault.setPayload(Map.of("password", "db-password"));
            vault.setFailReads(true);
            VaultHttp http = new VaultHttp(vault.url(), k8sOptions());

            assertThrows(SecurityException.class, () -> http.getSecretPayload("secret/data/db"));
            assertEquals(1, vault.reads(), "non-200 (except 403 with renewable token) must not be retried");
        }
    }

    @Test
    void tokenRefreshOn403ReauthenticatesAndRetriesOnce() throws Exception {
        try (VaultStubServer vault = VaultStubServer.start()) {
            vault.setPayload(Map.of("password", "db-password"));
            VaultHttp http = new VaultHttp(vault.url(), k8sOptions());

            assertEquals("db-password", http.getSecret("secret/data/db", "password"));
            assertEquals(1, vault.reads());
            assertEquals(1, vault.logins());

            vault.expireToken(); // token expired: the next read gets HTTP 403

            assertEquals("db-password", http.getSecret("secret/data/db", "password"));
            assertEquals(3, vault.reads(), "expected read + 403 + single retry");
            assertEquals(2, vault.logins(), "the 403 must trigger exactly one re-login");
            long refreshes = SecurityMetrics.value(SecurityMetrics.VAULT_TOKEN_REFRESH_TOTAL);
            assertTrue(refreshes >= 1, "vault_token_refresh_total must be counted");
        }
    }

    @Test
    void retryOn403HappensAtMostOnce() throws Exception {
        // Fixed-token mode: a re-authenticated client keeps receiving 403, so the
        // client must give up after exactly one retry (no retry storm).
        try (VaultStubServer vault = VaultStubServer.startWithFixedToken("never-matches")) {
            vault.setPayload(Map.of("password", "db-password"));
            VaultHttp http = new VaultHttp(vault.url(), k8sOptions());

            assertThrows(SecurityException.class, () -> http.getSecretPayload("secret/data/db"));
            assertEquals(2, vault.reads(), "initial attempt + one retry, nothing more");
            assertEquals(2, vault.logins(), "initial login + one re-login");
        }
    }

    @Test
    void explicitTokenIsUsedAsIsWithoutLogin() throws Exception {
        try (VaultStubServer vault = VaultStubServer.startWithFixedToken("explicit-token")) {
            vault.setPayload(Map.of("password", "db-password"));
            VaultHttp http = new VaultHttp(vault.url(), Map.of(), "explicit-token");

            assertEquals("db-password", http.getSecret("secret/data/db", "password"));
            assertEquals(0, vault.logins(), "an explicit token must not trigger Kubernetes auth");
        }
    }

    @Test
    void explicitTokenIsNotRefreshedOn403() throws Exception {
        try (VaultStubServer vault = VaultStubServer.startWithFixedToken("good-token")) {
            vault.setPayload(Map.of("password", "db-password"));
            VaultHttp http = new VaultHttp(vault.url(), Map.of(), "expired-token");

            assertThrows(SecurityException.class, () -> http.getSecretPayload("secret/data/db"));
            assertEquals(1, vault.reads(), "a static token must fail fast without re-auth");
            assertEquals(0, vault.logins());
        }
    }

    @Test
    void vaultRoleIsPassedOnLogin() throws Exception {
        try (VaultStubServer vault = VaultStubServer.start()) {
            vault.setPayload(Map.of("password", "db-password"));
            Map<String, String> options = Map.of(
                    VaultHttp.OPT_K8S_TOKEN_PATH, jwtFile,
                    VaultHttp.OPT_VAULT_ROLE, "custom-role");
            VaultHttp http = new VaultHttp(vault.url(), options);

            http.getSecretPayload("secret/data/db");

            assertEquals(1, vault.loginBodies().size());
            assertTrue(vault.loginBodies().get(0).contains("\"role\":\"custom-role\""),
                    "the configured role must reach the login request, got: "
                            + vault.loginBodies().get(0));
        }
    }

    @Test
    void missingTokenFailsWithClearMessage() {
        String absentTokenPath = tempDir.resolve("absent-" + System.nanoTime()).toString();
        SecurityException e = assertThrows(SecurityException.class,
                () -> new VaultHttp("http://127.0.0.1:1", Map.of(VaultHttp.OPT_K8S_TOKEN_PATH, absentTokenPath)));
        assertTrue(e.getMessage().contains("No Vault token available"));
    }
}
