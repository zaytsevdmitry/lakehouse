package org.lakehouse.security.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import org.apache.hadoop.conf.Configuration;
import org.lakehouse.security.LockboxHttp;
import org.lakehouse.security.SecretCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * AWS credentials provider that retrieves S3/MinIO access key and secret key
 * from Yandex Cloud Lockbox.
 *
 * <p>Configuration properties (in Hadoop {@code Configuration}):
 * <ul>
 *   <li>{@code fs.s3a.lockbox.secret-id} — Lockbox secret ID</li>
 *   <li>{@code fs.s3a.lockbox.access-key-secret} — key name for access key (default: {@code access_key})</li>
 *   <li>{@code fs.s3a.lockbox.secret-key-secret} — key name for secret key (default: {@code secret_key})</li>
 * </ul>
 *
 * <p>In spark-defaults.conf these are prefixed with {@code spark.hadoop.}:
 * <pre>
 * spark.hadoop.fs.s3a.lockbox.secret-id=eirvju...
 * spark.hadoop.fs.s3a.path.style.access=true
 * spark.hadoop.fs.s3a.impl=org.apache.hadoop.fs.s3a.S3AFileSystem
 * spark.hadoop.fs.s3a.impl.disable.cache=true
 * </pre>
 */
public class YcLockboxS3CredentialsProvider implements AWSCredentialsProvider {

    private static final Logger LOG = LoggerFactory.getLogger(YcLockboxS3CredentialsProvider.class);

    private static final String PREFIX = "fs.s3a.lockbox.";
    private static final String PROP_SECRET_ID = PREFIX + "secret-id";
    private static final String PROP_ACCESS_KEY = PREFIX + "access-key-secret";
    private static final String PROP_SECRET_KEY = PREFIX + "secret-key-secret";

    private final SecretCache cache = new SecretCache();
    private final Configuration conf;
    private final LockboxHttp lockbox;

    /**
     * Constructor called by Hadoop S3A via reflection.
     */
    public YcLockboxS3CredentialsProvider(URI uri, Configuration conf) {
        this.conf = conf;
        this.lockbox = new LockboxHttp();
        LOG.info("YcLockboxS3CredentialsProvider initialised");
    }

    @Override
    public AWSCredentials getCredentials() {
        String cacheKey = "yc:s3";
        String cached = cache.get(cacheKey);
        if (cached != null && cached.contains("|")) {
            String[] parts = cached.split("\\|", 2);
            return new BasicAWSCredentials(parts[0], parts[1]);
        }

        String secretId = conf.get(PROP_SECRET_ID);
        if (secretId == null || secretId.isEmpty()) {
            throw new SecurityException("YcLockboxS3CredentialsProvider: " + PROP_SECRET_ID + " not configured");
        }

        String accessKeyField = conf.get(PROP_ACCESS_KEY, "access_key");
        String secretKeyField = conf.get(PROP_SECRET_KEY, "secret_key");

        String accessKey = lockbox.getSecret(secretId, accessKeyField, null);
        String secretKey = lockbox.getSecret(secretId, secretKeyField, null);
        cache.put(cacheKey, accessKey + "|" + secretKey);
        return new BasicAWSCredentials(accessKey, secretKey);
    }

    @Override
    public void refresh() {
        cache.clear();
    }

    @Override
    public String toString() {
        return "YcLockboxS3CredentialsProvider";
    }
}
