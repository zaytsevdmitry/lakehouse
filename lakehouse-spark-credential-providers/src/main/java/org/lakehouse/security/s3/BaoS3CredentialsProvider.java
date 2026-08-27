package org.lakehouse.security.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import org.apache.hadoop.conf.Configuration;
import org.lakehouse.security.SecretCache;
import org.lakehouse.security.VaultHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * AWS credentials provider that retrieves S3/MinIO access key and secret key
 * from OpenBao / Vault.
 *
 * <p>Configuration properties (in Hadoop {@code Configuration}):
 * <ul>
 *   <li>{@code fs.s3a.bao.vault-url} — Vault HTTP API base URL</li>
 *   <li>{@code fs.s3a.bao.secret-path} — KV path, e.g. {@code secret/data/s3}</li>
 *   <li>{@code fs.s3a.bao.access-key-secret} — key name for access key (default: {@code access_key})</li>
 *   <li>{@code fs.s3a.bao.secret-key-secret} — key name for secret key (default: {@code secret_key})</li>
 * </ul>
 *
 * <p>In spark-defaults.conf these are prefixed with {@code spark.hadoop.}:
 * <pre>
 * spark.hadoop.fs.s3a.bao.vault-url=http://vault:8200
 * spark.hadoop.fs.s3a.bao.secret-path=secret/data/s3
 * spark.hadoop.fs.s3a.path.style.access=true
 * spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
 * spark.hadoop.fs.s3a.impl.disable.cache=true
 * </pre>
 */
public class BaoS3CredentialsProvider implements AWSCredentialsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(BaoS3CredentialsProvider.class);

    private static final String PREFIX = "fs.s3a.bao.";
    private static final String PROP_VAULT_URL = PREFIX + "vault-url";
    private static final String PROP_SECRET_PATH = PREFIX + "secret-path";
    private static final String PROP_ACCESS_KEY = PREFIX + "access-key-secret";
    private static final String PROP_SECRET_KEY = PREFIX + "secret-key-secret";

    private final SecretCache cache = new SecretCache();
    private final Configuration conf;
    private final VaultHttp vault;

    /**
     * Constructor called by Hadoop S3A via reflection.
     */
    public BaoS3CredentialsProvider(URI uri, Configuration conf) {
        this.conf = conf;
        String vaultUrl = conf.get(PROP_VAULT_URL);
        if (vaultUrl == null || vaultUrl.isEmpty()) {
            throw new SecurityException("BaoS3CredentialsProvider: " + PROP_VAULT_URL + " not configured");
        }
        this.vault = new VaultHttp(vaultUrl, java.util.Map.of());
        LOG.info("BaoS3CredentialsProvider initialised");
    }

    @Override
    public AWSCredentials getCredentials() {
        String cacheKey = "bao:s3";
        String cached = cache.get(cacheKey);
        if (cached != null && cached.contains("|")) {
            String[] parts = cached.split("\\|", 2);
            return new BasicAWSCredentials(parts[0], parts[1]);
        }

        String secretPath = conf.get(PROP_SECRET_PATH);
        String accessKeyField = conf.get(PROP_ACCESS_KEY, "access_key");
        String secretKeyField = conf.get(PROP_SECRET_KEY, "secret_key");

        if (secretPath == null || secretPath.isEmpty()) {
            throw new SecurityException("BaoS3CredentialsProvider: " + PROP_SECRET_PATH + " not configured");
        }

        String accessKey = vault.getSecret(secretPath, accessKeyField);
        String secretKey = vault.getSecret(secretPath, secretKeyField);
        cache.put(cacheKey, accessKey + "|" + secretKey);
        return new BasicAWSCredentials(accessKey, secretKey);
    }

    @Override
    public void refresh() {
        cache.clear();
    }

    @Override
    public String toString() {
        return "BaoS3CredentialsProvider";
    }
}
