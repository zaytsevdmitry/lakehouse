package org.lakehouse.modeller.auth;

import org.lakehouse.modeller.config.ConfiguratorProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keycloak {@code urn:ietf:params:oauth:grant-type:token-exchange} helper used under the
 * {@code token-exchange} authorization strategy: the user's Keycloak access token (bearer
 * JWT) is exchanged for the target API token of the configured Git provider (GitLab/GitHub).
 * Exchanged tokens are cached per user.
 */
public class TokenExchangeHelper {

    private static final Logger logger = LoggerFactory.getLogger(TokenExchangeHelper.class);

    private final String tokenUrl;
    private final String audience;
    private final String clientId;
    private final String clientSecret;
    private final Map<String, CachedToken> cache = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public TokenExchangeHelper(ConfiguratorProperties properties) {
        ConfiguratorProperties.Security.OAuth2.ResourceServer.Jwt jwt =
                properties.getSecurity().getOauth2().getResourceServer().getJwt();
        ConfiguratorProperties.Security.OAuth2.Registration registration =
                properties.getSecurity().getOauth2().getClient().registration("lakehouse");
        String issuerUri = jwt.getIssuerUri();
        this.tokenUrl = issuerUri == null || issuerUri.isBlank()
                ? null
                : trim(issuerUri) + "/protocol/openid-connect/token";
        String envAudience = System.getenv("LAKEHOUSE_TOKEX_AUDIENCE");
        this.audience = envAudience != null && !envAudience.isBlank() ? envAudience : "account";
        this.clientId = registration == null ? null : registration.getClientId();
        this.clientSecret = registration == null ? null : registration.getClientSecret();
        if (tokenUrl == null)
            logger.warn("lakehouse.configurator.security.oauth2.resourceserver.jwt.issuer-uri is not configured; "
                    + "token-exchange will be unavailable");
    }

    public boolean isConfigured() {
        return tokenUrl != null;
    }

    /**
     * Exchanges the given subject access token for the git provider access token.
     */
    public String exchange(String subjectToken) {
        if (tokenUrl == null)
            throw new ForbiddenException("token-exchange is configured but "
                    + "lakehouse.configurator.security.oauth2.resourceserver.jwt.issuer-uri is missing");
        String cacheKey = String.valueOf(subjectToken.hashCode());
        CachedToken cached = cache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() < cached.expiresAtMs() - 30_000)
            return cached.token();
        String token = doExchange(subjectToken);
        cache.put(cacheKey, new CachedToken(token, System.currentTimeMillis() + 300_000));
        return token;
    }

    private String doExchange(String subjectToken) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        form.put("client_id", clientId == null ? "" : clientId);
        form.put("client_secret", clientSecret == null ? "" : clientSecret);
        form.put("subject_token", subjectToken);
        form.put("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");
        form.put("audience", audience);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(encodeForm(form)))
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200)
                throw new ForbiddenException("Token exchange failed with HTTP " + response.statusCode());
            String body = new String(response.body(), StandardCharsets.UTF_8);
            String accessToken = extract("access_token", body);
            if (accessToken == null)
                throw new ForbiddenException("Token exchange response did not contain access_token");
            return accessToken;
        } catch (IOException e) {
            throw new ForbiddenException("Token exchange failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ForbiddenException("Token exchange interrupted");
        }
    }

    private static String extract(String key, String json) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0)
            return null;
        int colon = json.indexOf(':', idx);
        int start = json.indexOf('"', colon + 1);
        int end = json.indexOf('"', start + 1);
        if (start < 0 || end < 0)
            return null;
        return json.substring(start + 1, end);
    }

    private static String encodeForm(Map<String, String> form) {
        StringJoiner joiner = new StringJoiner("&");
        form.forEach((k, v) -> joiner.add(URLEncoder.encode(k, StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(v == null ? "" : v, StandardCharsets.UTF_8)));
        return joiner.toString();
    }

    private static String trim(String uri) {
        return uri.replaceAll("/+$", "");
    }

    private record CachedToken(String token, long expiresAtMs) {
    }
}