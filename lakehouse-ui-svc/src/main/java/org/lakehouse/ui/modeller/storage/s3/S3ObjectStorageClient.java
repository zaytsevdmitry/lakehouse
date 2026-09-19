package org.lakehouse.ui.modeller.storage.s3;

import org.lakehouse.ui.modeller.storage.WorkspaceStorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Minimal S3-compatible REST client (AWS Signature v4) built on the JDK {@link HttpClient}.
 * Works against AWS S3, MinIO and S3-compatible gateways. Used to persist workspaces as
 * plain-text YAML objects in streaming mode, without holding heavy Git archives in RAM.
 */
public class S3ObjectStorageClient {

    private static final Logger logger = LoggerFactory.getLogger(S3ObjectStorageClient.class);
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private final String endpoint;
    private final String bucket;
    private final AwsSigV4Signer signer;
    private final HttpClient httpClient;

    public S3ObjectStorageClient(String endpoint, String bucket, String accessKey, String secretKey, String region) {
        this.endpoint = normalizeEndpoint(endpoint);
        this.bucket = bucket;
        this.signer = new AwsSigV4Signer(accessKey, secretKey, region);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    public String getBucket() {
        return bucket;
    }

    public byte[] getObject(String key) {
        requireKey(key);
        SignedRequest signed = sign("GET", "/" + bucket + "/" + key, null, new byte[0]);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uriFor(key, null))
                .header("Host", host())
                .header("Authorization", signed.authorization)
                .header("x-amz-date", signed.xAmzDate)
                .header("x-amz-content-sha256", signed.xAmzContentSha256)
                .GET()
                .build();
        HttpResponse<byte[]> response = send(request);
        if (response.statusCode() == 404)
            return null;
        if (response.statusCode() != 200)
            throw error(key, response);
        return response.body();
    }

    public void putObject(String key, byte[] content) {
        requireKey(key);
        SignedRequest signed = sign("PUT", "/" + bucket + "/" + key, null, content);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uriFor(key, null))
                .header("Host", host())
                .header("Authorization", signed.authorization)
                .header("Content-Type", "application/octet-stream")
                .header("x-amz-date", signed.xAmzDate)
                .header("x-amz-content-sha256", signed.xAmzContentSha256)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
                .build();
        HttpResponse<Void> response = sendVoid(request);
        if (response.statusCode() != 200 && response.statusCode() != 201)
            throw error(key, response);
    }

    public void deleteObject(String key) {
        requireKey(key);
        SignedRequest signed = sign("DELETE", "/" + bucket + "/" + key, null, new byte[0]);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uriFor(key, null))
                .header("Host", host())
                .header("Authorization", signed.authorization)
                .header("x-amz-date", signed.xAmzDate)
                .header("x-amz-content-sha256", signed.xAmzContentSha256)
                .DELETE()
                .build();
        HttpResponse<Void> response = sendVoid(request);
        if (response.statusCode() != 204 && response.statusCode() != 200)
            throw error(key, response);
    }

    /**
     * Lists object keys under the given prefix, following pagination.
     */
    public List<String> listObjects(String prefix) {
        List<String> keys = new ArrayList<>();
        String continuationToken = null;
        do {
            Map<String, String> query = new LinkedHashMap<>();
            query.put("list-type", "2");
            query.put("prefix", prefix);
            if (continuationToken != null)
                query.put("continuation-token", continuationToken);
            SignedRequest signed = sign("GET", "/" + bucket, query, new byte[0]);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uriFor(null, query))
                    .header("Host", host())
                    .header("Authorization", signed.authorization)
                    .header("x-amz-date", signed.xAmzDate)
                    .header("x-amz-content-sha256", signed.xAmzContentSha256)
                    .GET()
                    .build();
            HttpResponse<byte[]> response = send(request);
            if (response.statusCode() != 200)
                throw new WorkspaceStorageException("S3 list failed (" + response.statusCode() + "): "
                        + keyFromBody(response.body()));
            ListResult result = parseListBucketResult(response.body());
            keys.addAll(result.keys());
            continuationToken = result.nextContinuationToken();
        } while (continuationToken != null);
        return keys;
    }

    // ------------------------------------------------------------------
    // internals
    // ------------------------------------------------------------------

    private SignedRequest sign(String method, String canonicalUri, Map<String, String> query, byte[] payload) {
        Map<String, String> base = new LinkedHashMap<>();
        base.put("host", host());
        AwsSigV4Signer.SignatureResult result = signer.sign(method, canonicalUri, query, base, payload);
        return new SignedRequest(result.authorization, result.xAmzDate, result.xAmzContentSha256);
    }

    private String host() {
        URI uri = URI.create(endpoint);
        return uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
    }

    private URI uriFor(String key, Map<String, String> query) {
        StringBuilder sb = new StringBuilder(endpoint);
        if (!endpoint.endsWith("/"))
            sb.append('/');
        sb.append(bucket);
        if (key != null && !key.isBlank())
            sb.append('/').append(AwsSigV4Signer.uriEncode(key, true));
        if (query != null && !query.isEmpty()) {
            String joined = query.entrySet().stream()
                    .map(e -> AwsSigV4Signer.uriEncode(e.getKey(), false) + "=" + AwsSigV4Signer.uriEncode(e.getValue(), false))
                    .reduce((a, b) -> a + "&" + b).orElse("");
            if (!joined.isEmpty())
                sb.append('?').append(joined);
        }
        return URI.create(sb.toString());
    }

    private HttpResponse<byte[]> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new WorkspaceStorageException("S3 request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WorkspaceStorageException("S3 request interrupted", e);
        }
    }

    private HttpResponse<Void> sendVoid(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (IOException e) {
            throw new WorkspaceStorageException("S3 request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WorkspaceStorageException("S3 request interrupted", e);
        }
    }

    private WorkspaceStorageException error(String key, HttpResponse<?> response) {
        String body = response.body() instanceof byte[] bytes
                ? new String(bytes, StandardCharsets.UTF_8) : String.valueOf(response.body());
        return new WorkspaceStorageException("S3 " + key + " failed (" + response.statusCode() + "): "
                + (body.length() > 500 ? body.substring(0, 500) : body));
    }

    private static String keyFromBody(byte[] body) {
        if (body == null) return "";
        String text = new String(body, StandardCharsets.UTF_8).replaceAll("\\s+", " ").trim();
        return text.substring(0, Math.min(300, text.length()));
    }

    private static ListResult parseListBucketResult(byte[] body) {
        List<String> keys = new ArrayList<>();
        String nextContinuationToken = null;
        try {
            JsonNode root = MAPPER.readTree(new ByteArrayInputStream(body));
            JsonNode contents = root != null ? root.get("Contents") : null;
            if (contents != null && contents.isArray()) {
                for (JsonNode item : contents) {
                    JsonNode key = item.get("Key");
                    if (key != null)
                        keys.add(key.asText());
                }
            }
            JsonNode token = root != null ? root.get("NextContinuationToken") : null;
            if (token != null && !token.asText().isBlank())
                nextContinuationToken = token.asText();
        } catch (Exception jsonError) {
            // Fall back to the legacy XML ListBucket result used by some S3-compatible gateways.
            try {
                keys.clear();
                nextContinuationToken = parseXmlBody(body, keys);
            } catch (Exception xmlError) {
                throw new WorkspaceStorageException("Cannot parse S3 list response: "
                        + new String(body, StandardCharsets.UTF_8), jsonError);
            }
        }
        return new ListResult(keys, nextContinuationToken);
    }

    private static String parseXmlBody(byte[] body, List<String> keys) {
        String xml = new String(body, StandardCharsets.UTF_8);
        java.util.regex.Matcher keyMatcher = java.util.regex.Pattern.compile("<Key>([^<]+)</Key>").matcher(xml);
        while (keyMatcher.find())
            keys.add(keyMatcher.group(1));
        java.util.regex.Matcher tokenMatcher =
                java.util.regex.Pattern.compile("<NextContinuationToken>([^<]+)</NextContinuationToken>").matcher(xml);
        return tokenMatcher.find() ? tokenMatcher.group(1) : null;
    }

    private record ListResult(List<String> keys, String nextContinuationToken) {
    }

    private record SignedRequest(String authorization, String xAmzDate, String xAmzContentSha256) {
    }

    private void requireKey(String key) {
        if (key == null || key.isBlank())
            throw new IllegalArgumentException("Object key must not be blank");
    }

    private static String normalizeEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isBlank())
            throw new IllegalArgumentException("S3 endpoint must be configured (lakehouse.modeller.storage.s3.endpoint)");
        String normalized = endpoint.trim().replaceAll("/+$", "");
        if (!normalized.toLowerCase(Locale.ROOT).startsWith("http://")
                && !normalized.toLowerCase(Locale.ROOT).startsWith("https://"))
            normalized = "http://" + normalized;
        return normalized;
    }
}