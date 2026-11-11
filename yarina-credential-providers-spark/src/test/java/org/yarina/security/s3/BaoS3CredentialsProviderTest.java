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
package org.yarina.security.s3;

import com.amazonaws.auth.AWSCredentials;
import org.apache.hadoop.conf.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yarina.security.SecurityMetrics;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests of {@link BaoS3CredentialsProvider} against an in-process OpenBao stub.
 *
 * <p>The JVM-wide {@code CACHE} / {@code CLIENTS} of the provider are static, so tests
 * isolate themselves through unique stub ports (unique vault URLs) and unique secret
 * paths; metric assertions use deltas for the same reason.
 */
class BaoS3CredentialsProviderTest {

    @TempDir
    static Path tempDir;

    private static String jwtFile;

    @BeforeAll
    static void setUp() throws IOException {
        // The Kubernetes-auth path must not be shadowed by VAULT_TOKEN.
        String envToken = System.getenv("VAULT_TOKEN");
        Assumptions.assumeTrue(envToken == null || envToken.isBlank(),
                "VAULT_TOKEN is set in the environment; Kubernetes-auth tests are skipped");

        jwtFile = tempDir.resolve("sa-token").toString();
        Files.writeString(Path.of(jwtFile), "test-service-account-jwt", StandardCharsets.UTF_8);
    }

    /* Two provider instances with identical conf share the JVM-wide cache entry. */
    @Test
    void cacheIsSharedAcrossProviderInstances() throws Exception {
        try (VaultStubServer vault = VaultStubServer.start()) {
            vault.setPayload(Map.of("access_key", "AKIA-SHARED", "secret_key", "SK-SHARED"));
            long createdBefore = SecurityMetrics.value(SecurityMetrics.PROVIDER_INSTANCES_CREATED_TOTAL);

            BaoS3CredentialsProvider first = provider(vault, "secret/data/s3-shared");
            BaoS3CredentialsProvider second = provider(vault, "secret/data/s3-shared");

            assertEquals("AKIA-SHARED", first.getCredentials().getAWSAccessKeyId());
            assertEquals("AKIA-SHARED", second.getCredentials().getAWSAccessKeyId());

            assertEquals(1, vault.reads(),
                    "the second provider instance must reuse the JVM-wide cache entry");
            assertEquals(1, vault.logins(), "the Vault client must be shared across instances");
            assertEquals(2,
                    SecurityMetrics.value(SecurityMetrics.PROVIDER_INSTANCES_CREATED_TOTAL) - createdBefore,
                    "both instances must be counted in provider_instances_created_total");
        }
    }

    /* Different secret paths are different cache entries — no cross-bucket leakage. */
    @Test
    void differentSecretPathsDoNotShareCacheEntry() throws Exception {
        try (VaultStubServer vault = VaultStubServer.start()) {
            vault.setPayload("secret/data/s3-bucket-a",
                    Map.of("access_key", "AKIA-BUCKET-A", "secret_key", "SK-BUCKET-A"));
            vault.setPayload("secret/data/s3-bucket-b",
                    Map.of("access_key", "AKIA-BUCKET-B", "secret_key", "SK-BUCKET-B"));

            BaoS3CredentialsProvider bucketA = provider(vault, "secret/data/s3-bucket-a");
            BaoS3CredentialsProvider bucketB = provider(vault, "secret/data/s3-bucket-b");

            assertEquals("AKIA-BUCKET-A", bucketA.getCredentials().getAWSAccessKeyId());
            assertEquals("AKIA-BUCKET-B", bucketB.getCredentials().getAWSAccessKeyId());
            assertEquals(2, vault.reads(), "different secret paths need their own cache entries");
        }
    }

    /* Different vault URLs never share a client nor credentials. */
    @Test
    void differentVaultUrlsUseDifferentClients() throws Exception {
        try (VaultStubServer vaultA = VaultStubServer.start();
                VaultStubServer vaultB = VaultStubServer.start()) {
            vaultA.setPayload(Map.of("access_key", "AKIA-VAULT-A", "secret_key", "SK-VAULT-A"));
            vaultB.setPayload(Map.of("access_key", "AKIA-VAULT-B", "secret_key", "SK-VAULT-B"));

            BaoS3CredentialsProvider fromA = provider(vaultA, "secret/data/s3-multi-url");
            BaoS3CredentialsProvider fromB = provider(vaultB, "secret/data/s3-multi-url");

            assertEquals("AKIA-VAULT-A", fromA.getCredentials().getAWSAccessKeyId());
            assertEquals("AKIA-VAULT-B", fromB.getCredentials().getAWSAccessKeyId());
            assertEquals(1, vaultA.logins());
            assertEquals(1, vaultB.logins(), "each Vault URL must get its own client");
            assertEquals(1, vaultA.reads());
            assertEquals(1, vaultB.reads());
        }
    }

    /* refresh() of one provider invalidates only its own (vaultUrl, secretPath). */
    @Test
    void refreshInvalidatesOnlyOwnKey() throws Exception {
        try (VaultStubServer vault = VaultStubServer.start()) {
            String pathA = "secret/data/s3-refresh-a";
            String pathB = "secret/data/s3-refresh-b";
            vault.setPayload(pathA, Map.of("access_key", "AKIA-A1", "secret_key", "SK-A1"));
            vault.setPayload(pathB, Map.of("access_key", "AKIA-B1", "secret_key", "SK-B1"));

            BaoS3CredentialsProvider providerA = provider(vault, pathA);
            BaoS3CredentialsProvider providerB = provider(vault, pathB);
            providerA.getCredentials();
            providerB.getCredentials();
            assertEquals(2, vault.reads());

            // Rotate A's secret and invalidate only A.
            vault.setPayload(pathA, Map.of("access_key", "AKIA-A2", "secret_key", "SK-A2"));
            providerA.refresh();

            assertEquals("AKIA-A2", providerA.getCredentials().getAWSAccessKeyId(),
                    "refresh() must force a re-fetch for its own key");
            assertEquals("AKIA-B1", providerB.getCredentials().getAWSAccessKeyId(),
                    "the neighbouring bucket must stay cached");
            assertEquals(3, vault.reads(), "only the invalidated entry is re-fetched");
            assertEquals(1, vault.logins());
        }
    }

    /* 50 concurrent misses cause a single Vault read (single-flight), not 50. */
    @Test
    void concurrentMissDoesNotCauseThunderingHerd() throws Exception {
        try (VaultStubServer vault = VaultStubServer.start()) {
            vault.setPayload(Map.of("access_key", "AKIA-HERD", "secret_key", "SK-HERD"));
            int threads = 50;

            ExecutorService pool = Executors.newFixedThreadPool(threads);
            try {
                CountDownLatch start = new CountDownLatch(1);
                List<Future<AWSCredentials>> results = new ArrayList<>(threads);
                for (int i = 0; i < threads; i++) {
                    results.add(pool.submit(() -> {
                        start.await();
                        BaoS3CredentialsProvider provider = provider(vault, "secret/data/s3-herd");
                        return provider.getCredentials();
                    }));
                }
                start.countDown();
                for (Future<AWSCredentials> result : results) {
                    assertEquals("AKIA-HERD", result.get(30, TimeUnit.SECONDS).getAWSAccessKeyId());
                }

                assertEquals(1, vault.reads(),
                        "single-flight: 50 concurrent misses must cause exactly one Vault read");
                assertEquals(1, vault.logins(),
                        "the Vault client must be created once for the whole herd");
            } finally {
                pool.shutdownNow();
            }
        }
    }

    /* vault-role from the Hadoop Configuration reaches the Vault client login. */
    @Test
    void vaultRoleIsPassedToHttpClient() throws Exception {
        try (VaultStubServer vault = VaultStubServer.start()) {
            vault.setPayload(Map.of("access_key", "AKIA-ROLE", "secret_key", "SK-ROLE"));
            Configuration conf = conf(vault.url(), "secret/data/s3-role");
            conf.set("fs.s3a.bao.vault-role", "custom-s3-role");

            new BaoS3CredentialsProvider(URI.create("s3a://bucket/"), conf).getCredentials();

            assertEquals(1, vault.loginBodies().size());
            assertTrue(vault.loginBodies().get(0).contains("\"role\":\"custom-s3-role\""),
                    "the configured role must reach the login request, got: "
                            + vault.loginBodies().get(0));
        }
    }

    /* vault-url comes from the Hadoop Configuration, not from env/system properties. */
    @Test
    void vaultUrlComesFromHadoopConfNotEnv() throws Exception {
        try (VaultStubServer configuredVault = VaultStubServer.start();
                VaultStubServer otherVault = VaultStubServer.start()) {
            configuredVault.setPayload(Map.of("access_key", "AKIA-CONF", "secret_key", "SK-CONF"));
            otherVault.setPayload(Map.of("access_key", "AKIA-OTHER", "secret_key", "SK-OTHER"));

            System.setProperty("yarina.vault.url", otherVault.url());
            System.setProperty("VAULT_ADDR", otherVault.url());
            try {
                BaoS3CredentialsProvider provider =
                        provider(configuredVault, "secret/data/s3-conf-url");

                assertEquals("AKIA-CONF", provider.getCredentials().getAWSAccessKeyId(),
                        "the Configuration value must win over env/system properties");
                assertEquals(1, configuredVault.reads());
                assertEquals(0, otherVault.reads(), "the sysprop/env Vault must never be contacted");
                assertEquals(0, otherVault.logins());
            } finally {
                System.clearProperty("yarina.vault.url");
                System.clearProperty("VAULT_ADDR");
            }
        }
    }

    /* The access/secret key pair is a single HTTP request (one batch read). */
    @Test
    void batchFetchMakesSingleHttpCall() throws Exception {
        try (VaultStubServer vault = VaultStubServer.start()) {
            vault.setPayload(Map.of(
                    "access_key", "AKIA-BATCH",
                    "secret_key", "SK-BATCH",
                    "unrelated", "ignored"));
            BaoS3CredentialsProvider provider = provider(vault, "secret/data/s3-batch");

            AWSCredentials credentials = provider.getCredentials();

            assertEquals("AKIA-BATCH", credentials.getAWSAccessKeyId());
            assertEquals("SK-BATCH", credentials.getAWSSecretKey());
            assertEquals(1, vault.reads(),
                    "the access/secret key pair must be fetched with a single HTTP request");
        }
    }

    /* A 403 from an expired token triggers one re-auth and exactly one retry. */
    @Test
    void tokenRefreshOn403() throws Exception {
        try (VaultStubServer vault = VaultStubServer.start()) {
            vault.setPayload(Map.of("access_key", "AKIA-403", "secret_key", "SK-403"));
            BaoS3CredentialsProvider provider = provider(vault, "secret/data/s3-403");

            assertEquals("AKIA-403", provider.getCredentials().getAWSAccessKeyId());
            assertEquals(1, vault.reads());
            assertEquals(1, vault.logins());

            vault.expireToken(); // the token is rejected from now on
            provider.refresh(); // drop the cached pair so the next call hits Vault

            assertEquals("AKIA-403", provider.getCredentials().getAWSAccessKeyId());
            assertEquals(3, vault.reads(),
                    "expected the rejected read plus exactly one retry");
            assertEquals(2, vault.logins(), "the 403 must trigger exactly one re-login");
            long refreshes = SecurityMetrics.value(SecurityMetrics.VAULT_TOKEN_REFRESH_TOTAL);
            assertTrue(refreshes >= 1, "vault_token_refresh_total must be counted");
        }
    }

    /* A failed fetch is never cached: the next attempt goes to Vault again. */
    @Test
    void failedFetchIsNotCached() throws Exception {
        try (VaultStubServer vault = VaultStubServer.start()) {
            vault.setPayload(Map.of("access_key", "AKIA-RETRY", "secret_key", "SK-RETRY"));
            BaoS3CredentialsProvider provider = provider(vault, "secret/data/s3-fail");

            vault.setFailReads(true); // every read returns HTTP 500
            assertThrows(SecurityException.class, provider::getCredentials);
            assertEquals(1, vault.reads());
            assertEquals(1, vault.logins());

            vault.setFailReads(false);
            AWSCredentials credentials = provider.getCredentials();
            assertEquals("AKIA-RETRY", credentials.getAWSAccessKeyId());
            assertEquals(2, vault.reads(), "errors must not be cached in the credentials cache");
            assertEquals(1, vault.logins());
        }
    }

    /* The provider fails fast when vault-url / secret-path are not configured. */
    @Test
    void constructorFailsFastOnMissingConfiguration() {
        Configuration withoutVaultUrl = new Configuration(false);
        withoutVaultUrl.set("fs.s3a.bao.secret-path", "secret/data/s3");
        SecurityException missingUrl = assertThrows(SecurityException.class,
                () -> new BaoS3CredentialsProvider(URI.create("s3a://bucket/"), withoutVaultUrl));
        assertTrue(missingUrl.getMessage().contains("fs.s3a.bao.vault-url"));

        Configuration withoutSecretPath = new Configuration(false);
        withoutSecretPath.set("fs.s3a.bao.vault-url", "http://127.0.0.1:1");
        SecurityException missingPath = assertThrows(SecurityException.class,
                () -> new BaoS3CredentialsProvider(URI.create("s3a://bucket/"), withoutSecretPath));
        assertTrue(missingPath.getMessage().contains("fs.s3a.bao.secret-path"));
    }

    /* ------------------------------------------------------------------ */

    private static BaoS3CredentialsProvider provider(VaultStubServer vault, String secretPath) {
        return new BaoS3CredentialsProvider(
                URI.create("s3a://bucket/"), conf(vault.url(), secretPath));
    }

    private static Configuration conf(String vaultUrl, String secretPath) {
        Configuration conf = new Configuration(false);
        conf.set("fs.s3a.bao.vault-url", vaultUrl);
        conf.set("fs.s3a.bao.secret-path", secretPath);
        conf.set("fs.s3a.bao.vault-k8s-token-path", jwtFile);
        return conf;
    }
}
