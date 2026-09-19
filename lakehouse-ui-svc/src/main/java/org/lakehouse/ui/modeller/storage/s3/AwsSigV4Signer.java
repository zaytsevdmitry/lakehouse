package org.lakehouse.ui.modeller.storage.s3;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * AWS Signature Version 4 HTTP request signer (S3 service). Computes the
 * {@code Authorization} header and the final signed {@code x-amz-date} /
 * {@code x-amz-content-sha256} headers for S3 REST calls against AWS or MinIO.
 */
public final class AwsSigV4Signer {

    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final String SERVICE = "s3";
    private static final String TERMINATOR = "aws4_request";

    private final String accessKey;
    private final String secretKey;
    private final String region;

    public AwsSigV4Signer(String accessKey, String secretKey, String region) {
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;
    }

    /**
     * Signs the request and returns everything the caller must transmit: the
     * {@code Authorization} header value plus the authoritative {@code x-amz-date}
     * and {@code x-amz-content-sha256} header values (so the caller can never
     * desynchronise dates/hashes between the signature and the request).
     */
    public SignatureResult sign(String method, String canonicalUri, Map<String, String> queryParams,
                                Map<String, String> headers, byte[] payload, ZonedDateTime now) {
        String amzDate = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC).format(now);
        String dateStamp = amzDate.substring(0, 8);
        String host = headers.get("host");
        if (host == null)
            throw new IllegalArgumentException("'host' header is required for signing");

        TreeMap<String, String> sortedHeaders = new TreeMap<>(headers);
        String payloadHash = payload == null || payload.length == 0
                ? sha256Hex(new byte[0]) : sha256Hex(payload);
        sortedHeaders.put("x-amz-date", amzDate);
        sortedHeaders.put("x-amz-content-sha256", payloadHash);

        String canonicalRequest = canonicalRequest(method, canonicalUri, queryParams, sortedHeaders, payloadHash);
        String credentialScope = dateStamp + "/" + region + "/" + SERVICE + "/" + TERMINATOR;
        String stringToSign = ALGORITHM + "\n" + amzDate + "\n" + credentialScope + "\n"
                + sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        byte[] signingKey = signingKey(dateStamp);
        String signature = HexFormat.of().formatHex(hmacSha256(signingKey, stringToSign.getBytes(StandardCharsets.UTF_8)));

        String signedHeaders = String.join(";", sortedHeaders.keySet());
        String authorization = ALGORITHM + " Credential=" + accessKey + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + signature;
        return new SignatureResult(authorization, amzDate, payloadHash);
    }

    public SignatureResult sign(String method, String canonicalUri, Map<String, String> queryParams,
                                Map<String, String> headers, byte[] payload) {
        return sign(method, canonicalUri, queryParams, headers, payload, ZonedDateTime.now(ZoneOffset.UTC));
    }

    private String canonicalRequest(String method, String canonicalUri, Map<String, String> queryParams,
                                    Map<String, String> headers, String payloadHash) {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append('\n');
        sb.append(canonicalUri).append('\n');
        sb.append(canonicalQueryString(queryParams)).append('\n');
        StringBuilder canonicalHeaders = new StringBuilder();
        headers.forEach((name, value) ->
                canonicalHeaders.append(name.toLowerCase()).append(':').append(value.trim().replaceAll("\\s+", " ")).append('\n'));
        sb.append(canonicalHeaders);
        sb.append('\n');
        sb.append(String.join(";", headers.keySet())).append('\n');
        sb.append(payloadHash);
        return sb.toString();
    }

    private String canonicalQueryString(Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty())
            return "";
        TreeMap<String, String> sorted = new TreeMap<>(queryParams);
        StringBuilder sb = new StringBuilder();
        sorted.forEach((k, v) -> {
            if (sb.length() > 0)
                sb.append('&');
            sb.append(uriEncode(k, false)).append('=').append(uriEncode(v == null ? "" : v, false));
        });
        return sb.toString();
    }

    /**
     * RFC 3986 encoding used in AWS SigV4 canonical URIs and query strings.
     * Unreserved characters stay verbatim; with {@code keepSlash=true} the slash is
     * preserved (S3 object-key paths).
     */
    public static String uriEncode(String value, boolean keepSlash) {
        StringBuilder sb = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')
                    || c == '-' || c == '_' || c == '.' || c == '~'
                    || (keepSlash && c == '/')) {
                sb.append((char) c);
            } else {
                sb.append('%').append(HexFormat.of().withUpperCase().toHexDigits((byte) c));
            }
        }
        return sb.toString();
    }

    private byte[] signingKey(String dateStamp) {
        byte[] kDate = hmacSha256(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), dateStamp.getBytes(StandardCharsets.UTF_8));
        byte[] kRegion = hmacSha256(kDate, region.getBytes(StandardCharsets.UTF_8));
        byte[] kService = hmacSha256(kRegion, SERVICE.getBytes(StandardCharsets.UTF_8));
        return hmacSha256(kService, TERMINATOR.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(data));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA256 not available", e);
        }
    }

    public static final class SignatureResult {
        public final String authorization;
        public final String xAmzDate;
        public final String xAmzContentSha256;

        public SignatureResult(String authorization, String xAmzDate, String xAmzContentSha256) {
            this.authorization = authorization;
            this.xAmzDate = xAmzDate;
            this.xAmzContentSha256 = xAmzContentSha256;
        }
    }
}