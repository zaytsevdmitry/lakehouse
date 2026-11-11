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
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import org.apache.hadoop.conf.Configuration;
import org.yarina.security.SecretCache;
import org.yarina.security.SecurityMetrics;
import org.yarina.security.VaultHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AWS credentials provider that retrieves S3/MinIO access key and secret key
 * from OpenBao / Vault.
 *
 * <p><b>Cache and clients are JVM-wide.</b> Hadoop S3A may create many instances of
 * this provider inside a single JVM (one per {@code FileSystem} instance — and with
 * {@code fs.s3a.impl.disable.cache=true} every {@code FileSystem.get} creates a new
 * one). To keep the OpenBao load proportional to the TTL and not to the number of
 * provider instances, the credential cache and the Vault clients live in
 * {@code static} fields shared by all instances:
 *
 * <ul>
 *   <li>cache key — {@code "bao:s3:<vaultUrl>:<secretPath>"}, so different buckets
 *       (different secret paths) never share a cache entry;</li>
 *   <li>client key — {@code "<vaultUrl>|<vaultRole>|<k8sAuthPath>"}, so different
 *       Vault servers / Kubernetes auth roles never share a client;</li>
 *   <li>{@link #refresh()} invalidates only the entry of this provider's
 *       (vaultUrl, secretPath) — credentials of neighbouring buckets stay cached.</li>
 * </ul>
 *
 * <p>Both keys of a credential pair are fetched with a single HTTP request
 * ({@link VaultHttp#getSecretPayload(String)}). Errors are never cached.
 *
 * <p>Configuration properties (in Hadoop {@code Configuration}; bucket-scoped
 * {@code fs.s3a.bucket.<name>.*} variants are honoured by S3A):
 * <ul>
 *   <li>{@code fs.s3a.bao.vault-url} — Vault HTTP API base URL</li>
 *   <li>{@code fs.s3a.bao.secret-path} — KV path, e.g. {@code secret/data/s3}</li>
 *   <li>{@code fs.s3a.bao.access-key-secret} — key name for access key (default: {@code access_key})</li>
 *   <li>{@code fs.s3a.bao.secret-key-secret} — key name for secret key (default: {@code secret_key})</li>
 *   <li>{@code fs.s3a.bao.vault-role} — Kubernetes auth role (default: {@code yarina})</li>
 *   <li>{@code fs.s3a.bao.vault-k8s-auth-path} — Kubernetes auth mount path (default: {@code kubernetes})</li>
 *   <li>{@code fs.s3a.bao.vault-k8s-token-path} — service-account JWT path (default:
 *       {@code /var/run/secrets/kubernetes.io/serviceaccount/token})</li>
 * </ul>
 *
 * <p>In spark-defaults.conf these are prefixed with {@code spark.hadoop.}:
 * <pre>
 * spark.hadoop.fs.s3a.bao.vault-url=http://vault:8200
 * spark.hadoop.fs.s3a.bao.secret-path=secret/data/s3
 * spark.hadoop.fs.s3a.path.style.access=true
 * spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
 * </pre>
 */
public class BaoS3CredentialsProvider implements AWSCredentialsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(BaoS3CredentialsProvider.class);

    private static final String PREFIX = "fs.s3a.bao.";
    private static final String PROP_VAULT_URL = PREFIX + "vault-url";
    private static final String PROP_SECRET_PATH = PREFIX + "secret-path";
    private static final String PROP_ACCESS_KEY = PREFIX + "access-key-secret";
    private static final String PROP_SECRET_KEY = PREFIX + "secret-key-secret";
    private static final String PROP_VAULT_ROLE = PREFIX + "vault-role";
    private static final String PROP_K8S_AUTH_PATH = PREFIX + "vault-k8s-auth-path";
    private static final String PROP_K8S_TOKEN_PATH = PREFIX + "vault-k8s-token-path";

    private static final String DEFAULT_ACCESS_KEY_FIELD = "access_key";
    private static final String DEFAULT_SECRET_KEY_FIELD = "secret_key";
    private static final String DEFAULT_VAULT_ROLE = "yarina";
    private static final String DEFAULT_K8S_AUTH_PATH = "kubernetes";

    private static final String CREDENTIALS_SEPARATOR = "|";

    /** JVM-wide TTL cache shared by all provider instances. Key: bao:s3:&lt;vaultUrl&gt;:&lt;secretPath&gt;. */
    private static final SecretCache CACHE = new SecretCache();
    /** JVM-wide registry of Vault clients, created lazily. Key: &lt;vaultUrl&gt;|&lt;vaultRole&gt;|&lt;k8sAuthPath&gt;. */
    private static final Map<String, VaultHttp> CLIENTS = new ConcurrentHashMap<>();

    private final String vaultUrl;
    private final String secretPath;
    private final String accessKeyField;
    private final String secretKeyField;
    private final String cacheKey;
    private final String clientKey;
    private final Map<String, String> clientOptions;

    /**
     * Constructor called by Hadoop S3A via reflection.
     */
    public BaoS3CredentialsProvider(URI uri, Configuration conf) {
        this.vaultUrl = requireConfigured(conf, PROP_VAULT_URL);
        this.secretPath = requireConfigured(conf, PROP_SECRET_PATH);
        this.accessKeyField = conf.get(PROP_ACCESS_KEY, DEFAULT_ACCESS_KEY_FIELD);
        this.secretKeyField = conf.get(PROP_SECRET_KEY, DEFAULT_SECRET_KEY_FIELD);

        String vaultRole = conf.get(PROP_VAULT_ROLE, DEFAULT_VAULT_ROLE);
        String authPath = conf.get(PROP_K8S_AUTH_PATH, DEFAULT_K8S_AUTH_PATH);
        Map<String, String> opts = new HashMap<>();
        opts.put(VaultHttp.OPT_VAULT_ROLE, vaultRole);
        opts.put(VaultHttp.OPT_K8S_AUTH_PATH, authPath);
        String k8sTokenPath = conf.get(PROP_K8S_TOKEN_PATH);
        if (k8sTokenPath != null && !k8sTokenPath.isEmpty()) {
            opts.put(VaultHttp.OPT_K8S_TOKEN_PATH, k8sTokenPath);
        }
        this.clientOptions = Map.copyOf(opts);

        this.cacheKey = "bao:s3:" + vaultUrl + ":" + secretPath;
        this.clientKey = vaultUrl + "|" + vaultRole + "|" + authPath;

        SecurityMetrics.increment(SecurityMetrics.PROVIDER_INSTANCES_CREATED_TOTAL);
        LOG.info("BaoS3CredentialsProvider initialised");
    }

    @Override
    public AWSCredentials getCredentials() {
        // Single-flight on a cache miss: concurrent partitions/tasks cause one Vault request.
        String pair = CACHE.computeIfAbsent(cacheKey, key -> fetchCredentialPair());
        if (pair == null || !pair.contains(CREDENTIALS_SEPARATOR)) {
            throw new SecurityException(
                    "BaoS3CredentialsProvider: credentials could not be resolved from Vault");
        }
        String[] parts = pair.split("\\" + CREDENTIALS_SEPARATOR, 2);
        return new BasicAWSCredentials(parts[0], parts[1]);
    }

    @Override
    public void refresh() {
        // Invalidate only this provider's (vaultUrl, secretPath); neighbouring
        // buckets/vaults keep their cached credentials.
        CACHE.invalidate(cacheKey);
    }

    @Override
    public String toString() {
        return "BaoS3CredentialsProvider";
    }

    /* ------------------------------------------------------------------ */

    /**
     * Fetch both credential fields with a single HTTP request to Vault.
     * Executed under the SecretCache single-flight slot; failures are not cached.
     */
    private String fetchCredentialPair() {
        SecurityMetrics.increment(SecurityMetrics.VAULT_REQUESTS_TOTAL, SecurityMetrics.LABEL_OP, "read");
        Map<String, String> payload = vaultClient().getSecretPayload(secretPath);
        String accessKey = requirePayloadKey(payload, accessKeyField);
        String secretKey = requirePayloadKey(payload, secretKeyField);
        return accessKey + CREDENTIALS_SEPARATOR + secretKey;
    }

    private VaultHttp vaultClient() {
        VaultHttp client = CLIENTS.get(clientKey);
        if (client == null) {
            client = CLIENTS.computeIfAbsent(clientKey, key -> {
                SecurityMetrics.increment(SecurityMetrics.VAULT_REQUESTS_TOTAL,
                        SecurityMetrics.LABEL_OP, "auth");
                LOG.info("Creating Vault client for [redacted]");
                return new VaultHttp(vaultUrl, clientOptions);
            });
        }
        return client;
    }

    private static String requirePayloadKey(Map<String, String> payload, String keyName) {
        String value = payload.get(keyName);
        if (value == null || value.isEmpty()) {
            throw new SecurityException(
                    "BaoS3CredentialsProvider: secret key '" + keyName + "' not found in Vault payload");
        }
        return value;
    }

    private static String requireConfigured(Configuration conf, String key) {
        String value = conf.get(key);
        if (value == null || value.isEmpty()) {
            throw new SecurityException("BaoS3CredentialsProvider: " + key + " not configured");
        }
        return value;
    }
}
