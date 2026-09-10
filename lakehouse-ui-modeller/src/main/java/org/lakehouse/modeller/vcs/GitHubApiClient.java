package org.lakehouse.modeller.vcs;

import org.lakehouse.modeller.config.ConfiguratorProperties;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GitHub App integration used by {@link GitHubAppVcsProvider}: builds a short-lived App JWT
 * from the private key, exchanges it for an installation access token via the REST API,
 * pushes through that token (username {@code x-access-token}) and opens a pull request
 * after review submission.
 */
public class GitHubApiClient {

    private final ConfiguratorProperties properties;
    private final RestSupport http = new RestSupport();
    private final String owner;
    private final String repo;

    public GitHubApiClient(ConfiguratorProperties properties) {
        this.properties = properties;
        String remoteUrl = properties.getGit().getRemoteUrl();
        if (remoteUrl == null || remoteUrl.isBlank())
            throw new VcsProviderException("No GitHub remote URL configured (lakehouse.configurator.git.remote-url)");
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(?:github\\.com[/:])([^/]+)/([^/.]+)(?:\\.git)?$")
                .matcher(remoteUrl.replaceAll("git@", ""));
        if (!m.find())
            throw new VcsProviderException("Cannot parse GitHub owner/repo from remote URL: " + remoteUrl);
        this.owner = m.group(1);
        this.repo = m.group(2);
    }

    public String installationAccessToken() {
        String appJwt = appJwt();
        String body = http.post("https://api.github.com/app/installations/"
                        + properties.getGithub().getInstallationId() + "/access_tokens",
                "{}", "Bearer " + appJwt).body();
        String token = RestSupport.parseMap(body).getString("token");
        if (token == null)
            throw new VcsProviderException("GitHub installation access token missing from response");
        return token;
    }

    public VcsReviewResult openPullRequest(String headBranch, String baseBranch, String description) {
        String token = installationAccessToken();
        Map<String, String> body = new LinkedHashMap<>();
        body.put("title", "Review request: " + headBranch + " -> " + baseBranch);
        body.put("head", headBranch);
        body.put("base", baseBranch);
        body.put("body", description == null ? "" : description);
        var response = http.post("https://api.github.com/repos/" + owner + "/" + repo + "/pulls",
                RestSupport.toJson(body), "Bearer " + token);
        if (response.statusCode() == 422) {
            // head or base differs → existing PR being updated
            return VcsReviewResult.updated("https://github.com/" + owner + "/" + repo + "/pulls?q="
                    + RestSupport.encode("head:" + headBranch));
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300)
            throw new VcsProviderException("GitHub PR create failed (" + response.statusCode() + "): " + response.body());
        return VcsReviewResult.created(RestSupport.parseMap(response.body()).getString("html_url"));
    }

    public String pushPassword() {
        return installationAccessToken();
    }

    public String pushUsername() {
        return "x-access-token";
    }

    private String appJwt() {
        try {
            Path keyPath = Path.of(properties.getGithub().getAppPrivateKeyPath());
            List<String> lines = Files.readAllLines(keyPath, StandardCharsets.UTF_8);
            String pem = String.join("", lines)
                    .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                    .replace("-----END RSA PRIVATE KEY-----", "")
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(pem);
            PrivateKey key;
            try {
                PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
                key = KeyFactory.getInstance("RSA").generatePrivate(spec);
            } catch (Exception pkcs8Failure) {
                // OpenSSL "BEGIN RSA PRIVATE KEY" (PKCS#1) — wrap into a PKCS#8 envelope.
                byte[] pkcs8 = pkcs1Envelope(der);
                key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(pkcs8));
            }
            long now = System.currentTimeMillis() / 1000;
            String header = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
            String payload = base64Url("{\"iat\":" + now + ",\"exp\":" + (now + 540)
                    + ",\"iss\":\"" + properties.getGithub().getAppId() + "\"}");
            String signingInput = header + "." + payload;
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(key);
            signature.update(signingInput.getBytes(StandardCharsets.US_ASCII));
            return signingInput + "." + base64Url(java.util.Base64.getEncoder().encode(signature.sign()));
        } catch (Exception e) {
            throw new VcsProviderException("Cannot build GitHub App JWT: " + e.getMessage(), e);
        }
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String base64Url(byte[] raw) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }

    private static byte[] pkcs1Envelope(byte[] pkcs1) {
        // PKCS#8 PrivateKeyInfo wrapping the PKCS#1 RSAPrivateKey DER payload:
        // SEQUENCE { INTEGER 0, SEQUENCE { oid rsaEncryption, NULL }, OCTET STRING pkcs1 }
        final byte[] algorithmId = {
                0x30, 0x0d,
                0x06, 0x09, 0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01,
                0x05, 0x00
        };
        byte version = 0x02, versionValue = 0x01, versionZero = 0x00;
        byte[] body = concat(new byte[]{version, versionValue, versionZero}, algorithmId, octetString(pkcs1));
        return concat(new byte[]{0x30}, lengthBytes(body.length), body);
    }

    private static byte[] octetString(byte[] inner) {
        return concat(new byte[]{0x04}, lengthBytes(inner.length), inner);
    }

    private static byte[] lengthBytes(int length) {
        if (length < 0x80)
            return new byte[]{(byte) length};
        int bytes = 0;
        int value = length;
        while (value > 0) {
            bytes++;
            value >>>= 8;
        }
        byte[] result = new byte[bytes + 1];
        result[0] = (byte) (0x80 | bytes);
        for (int i = 0; i < bytes; i++)
            result[bytes - i] = (byte) (length >>> (8 * i));
        return result;
    }

    private static byte[] concat(byte[]... arrays) {
        int total = 0;
        for (byte[] array : arrays)
            total += array.length;
        byte[] result = new byte[total];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}