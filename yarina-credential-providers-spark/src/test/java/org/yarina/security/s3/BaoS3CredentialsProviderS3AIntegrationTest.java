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

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yarina.security.SecurityMetrics;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test reproducing the production incident shape: Spark configmaps
 * contained {@code spark.hadoop.fs.s3a.impl.disable.cache=true}, so every
 * {@code FileSystem.get} created a fresh {@code S3AFileSystem} — and, before the
 * fix, a fresh credential cache, multiplying OpenBao reads by the number of
 * filesystem instances.
 *
 * <p>With the JVM-wide cache the load stays proportional to the TTL: 100 filesystem
 * instances (each with its own provider instance) issue at most two Vault reads
 * and a single Kubernetes login.
 */
class BaoS3CredentialsProviderS3AIntegrationTest {

    private static final int FILESYSTEM_INSTANCES = 100;

    @TempDir
    static Path tempDir;

    private static String jwtFile;

    @BeforeAll
    static void setUp() throws IOException {
        String envToken = System.getenv("VAULT_TOKEN");
        Assumptions.assumeTrue(envToken == null || envToken.isBlank(),
                "VAULT_TOKEN is set in the environment; Kubernetes-auth tests are skipped");

        jwtFile = tempDir.resolve("sa-token").toString();
        Files.writeString(Path.of(jwtFile), "test-service-account-jwt", StandardCharsets.UTF_8);
    }

    @Test
    void s3aFileSystemReuseDoesNotMultiplyVaultCalls() throws Exception {
        try (VaultStubServer vault = VaultStubServer.start();
                S3StubServer s3 = S3StubServer.start()) {
            vault.setPayload(java.util.Map.of(
                    "access_key", "AKIA-INTEGRATION", "secret_key", "SK-INTEGRATION"));
            long createdBefore = SecurityMetrics.value(SecurityMetrics.PROVIDER_INSTANCES_CREATED_TOTAL);

            Configuration conf = new Configuration();
            conf.set("fs.s3a.endpoint", s3.url());
            conf.set("fs.s3a.endpoint.region", "us-east-1");
            conf.set("fs.s3a.path.style.access", "true");
            conf.set("fs.s3a.bucket.probe", "0");
            conf.set("fs.s3a.aws.credentials.provider", BaoS3CredentialsProvider.class.getName());
            conf.set("fs.s3a.impl.disable.cache", "true");
            conf.set("fs.s3a.bao.vault-url", vault.url());
            conf.set("fs.s3a.bao.secret-path",
                    "secret/data/s3-integration-" + System.nanoTime());
            conf.set("fs.s3a.bao.vault-k8s-token-path", jwtFile);

            URI bucket = URI.create("s3a://stub-bucket/");
            for (int i = 0; i < FILESYSTEM_INSTANCES; i++) {
                FileSystem fs = FileSystem.get(bucket, conf);
                try {
                    // A signed request forces S3A to ask the provider for credentials;
                    // the stub answers 404 + empty listing -> "not found".
                    int index = i;
                    assertThrows(FileNotFoundException.class,
                            () -> fs.getFileStatus(
                                    new org.apache.hadoop.fs.Path("s3a://stub-bucket/missing-" + index)));
                } finally {
                    fs.close();
                }
            }

            assertTrue(s3.requests() > 0, "the S3 stub must have received signed requests");
            assertEquals(FILESYSTEM_INSTANCES,
                    SecurityMetrics.value(SecurityMetrics.PROVIDER_INSTANCES_CREATED_TOTAL) - createdBefore,
                    "disable.cache=true must really create one provider per FileSystem.get");
            assertTrue(vault.reads() <= 2,
                    "the JVM-wide cache must keep Vault reads proportional to the TTL, got: "
                            + vault.reads());
            assertEquals(1, vault.logins(),
                    "the Vault client must be created once per JVM, not per filesystem instance");
        }
    }
}
