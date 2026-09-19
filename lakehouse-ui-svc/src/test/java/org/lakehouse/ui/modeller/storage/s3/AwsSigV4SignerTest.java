package org.lakehouse.ui.modeller.storage.s3;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AwsSigV4SignerTest {

    private static final String ACCESS_KEY = "AKIAIOSFODNN7EXAMPLE";
    private static final String SECRET_KEY = "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY";
    private static final String EMPTY_PAYLOAD_HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @Test
    void signatureMatchesTheAwsCanonicalRequestConstruction() {
        // Validates against the AWS SigV4 formulation directly, including the required
        // blank line between the canonical header block and the signed-header list.
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("host", "examplebucket.s3.amazonaws.com");
        AwsSigV4Signer signer = new AwsSigV4Signer(ACCESS_KEY, SECRET_KEY, "us-east-1");
        ZonedDateTime now = ZonedDateTime.of(2013, 5, 24, 0, 0, 0, 0, ZoneOffset.UTC);
        AwsSigV4Signer.SignatureResult result = signer.sign("GET", "/test.txt", Map.of(), headers, new byte[0], now);

        String payloadHash = EMPTY_PAYLOAD_HASH;
        String canonicalRequest = "GET\n/test.txt\n\n"
                + "host:examplebucket.s3.amazonaws.com\n"
                + "x-amz-content-sha256:" + payloadHash + "\n"
                + "x-amz-date:20130524T000000Z\n\n"
                + "host;x-amz-content-sha256;x-amz-date\n"
                + payloadHash;
        String expectedSignature = expectedSignature(canonicalRequest, payloadHash);

        assertThat(result.xAmzContentSha256).isEqualTo(payloadHash);
        assertThat(result.xAmzDate).isEqualTo("20130524T000000Z");
        assertThat(result.authorization).isEqualTo(
                "AWS4-HMAC-SHA256 Credential=" + ACCESS_KEY + "/20130524/us-east-1/s3/aws4_request,"
                        + " SignedHeaders=host;x-amz-content-sha256;x-amz-date,"
                        + " Signature=" + expectedSignature);
    }

    @Test
    void changingHeaderSetChangesTheSignature() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("host", "examplebucket.s3.amazonaws.com");
        AwsSigV4Signer signer = new AwsSigV4Signer(ACCESS_KEY, SECRET_KEY, "us-east-1");
        ZonedDateTime now = ZonedDateTime.of(2013, 5, 24, 0, 0, 0, 0, ZoneOffset.UTC);

        AwsSigV4Signer.SignatureResult plain = signer.sign("GET", "/test.txt", Map.of(), headers, new byte[0], now);
        AwsSigV4Signer.SignatureResult withAcl = signer.sign("GET", "/test.txt", Map.of(),
                new LinkedHashMap<>(Map.of("host", "examplebucket.s3.amazonaws.com", "x-amz-acl", "private")),
                new byte[0], now);

        assertThat(plain.authorization).contains("SignedHeaders=host;x-amz-content-sha256;x-amz-date");
        assertThat(withAcl.authorization).contains("SignedHeaders=host;x-amz-acl;x-amz-content-sha256;x-amz-date");
        assertThat(withAcl.authorization).isNotEqualTo(plain.authorization);
    }

    @Test
    void signsNonEmptyPayloadIncludingKnownSha256Vector() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("host", "examplebucket.s3.amazonaws.com");
        AwsSigV4Signer.SignatureResult result = new AwsSigV4Signer(ACCESS_KEY, SECRET_KEY, "us-east-1")
                .sign("PUT", "/test.txt", Map.of(), headers,
                        "Welcome to Amazon S3.".getBytes(StandardCharsets.UTF_8),
                        ZonedDateTime.of(2013, 5, 24, 0, 0, 0, 0, ZoneOffset.UTC));

        // sha256("Welcome to Amazon S3.") as used in the AWS documentation example.
        assertThat(result.xAmzContentSha256)
                .isEqualTo("44ce7dd67c959e0d3524ffac1771dfbba87d2b6b4b4e99e42034a8b803f8b072");
        assertThat(result.authorization).startsWith("AWS4-HMAC-SHA256 Credential=" + ACCESS_KEY + "/20130524/us-east-1/s3/aws4_request");
    }

    @Test
    void uriEncodeEscapesNonUnreservedAndKeepsSlashOnRequest() {
        assertThat(AwsSigV4Signer.uriEncode("a b/c~d", false)).isEqualTo("a%20b%2Fc~d");
        assertThat(AwsSigV4Signer.uriEncode("a b/c~d", true)).isEqualTo("a%20b/c~d");
        assertThat(AwsSigV4Signer.uriEncode("config/namespace/ns.yaml", true))
                .isEqualTo("config/namespace/ns.yaml");
    }

    @Test
    void canonicalQueryIsSortedAndEncoded() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("host", "examplebucket.s3.amazonaws.com");
        AwsSigV4Signer.SignatureResult result = new AwsSigV4Signer(ACCESS_KEY, SECRET_KEY, "us-east-1")
                .sign("GET", "/test.txt", Map.of("z", "1", "a", "2 3"), headers, new byte[0],
                        ZonedDateTime.of(2013, 5, 24, 0, 0, 0, 0, ZoneOffset.UTC));

        // The query string is ordered and percent-encoded before being hashed; assert stability
        // by signing with reversed insertion order and comparing authorization headers.
        AwsSigV4Signer.SignatureResult reverse = new AwsSigV4Signer(ACCESS_KEY, SECRET_KEY, "us-east-1")
                .sign("GET", "/test.txt", Map.of("a", "2 3", "z", "1"), headers, new byte[0],
                        ZonedDateTime.of(2013, 5, 24, 0, 0, 0, 0, ZoneOffset.UTC));
        assertThat(result.authorization).isEqualTo(reverse.authorization);
    }

    @Test
    void emptyQueryAndMissingHostAreHandled() {
        assertThatThrownBy(() -> new AwsSigV4Signer(ACCESS_KEY, SECRET_KEY, "us-east-1")
                .sign("GET", "/test.txt", null, Map.of(), new byte[0]))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("host");
    }

    private static String expectedSignature(String canonicalRequest, String payloadHash) {
        String credentialScope = "20130524/us-east-1/s3/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n20130524T000000Z\n" + credentialScope + "\n"
                + AwsSigV4Signer.sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        byte[] kDate = AwsSigV4Signer.hmacSha256(
                ("AWS4" + SECRET_KEY).getBytes(StandardCharsets.UTF_8), "20130524".getBytes(StandardCharsets.UTF_8));
        byte[] kRegion = AwsSigV4Signer.hmacSha256(kDate, "us-east-1".getBytes(StandardCharsets.UTF_8));
        byte[] kService = AwsSigV4Signer.hmacSha256(kRegion, "s3".getBytes(StandardCharsets.UTF_8));
        byte[] kSigning = AwsSigV4Signer.hmacSha256(kService, "aws4_request".getBytes(StandardCharsets.UTF_8));
        return java.util.HexFormat.of().formatHex(
                AwsSigV4Signer.hmacSha256(kSigning, stringToSign.getBytes(StandardCharsets.UTF_8)));
    }
}