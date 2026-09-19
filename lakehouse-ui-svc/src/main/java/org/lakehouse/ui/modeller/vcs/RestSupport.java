package org.lakehouse.ui.modeller.vcs;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Shared, dependency-light HTTP + JSON helpers for the Git provider REST integrations
 * (GitLab API, GitHub App).
 */
public final class RestSupport {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public HttpResponse<String> get(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        return sendString(request, url);
    }

    public HttpResponse<String> get(String url, String authHeader) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Authorization", authHeader)
                .GET()
                .build();
        return sendString(request, url);
    }

    public HttpResponse<byte[]> getBytes(String url) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "text/plain")
                .GET()
                .build();
        try {
            return CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new VcsProviderException("HTTP GET failed for " + url + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VcsProviderException("HTTP GET interrupted for " + url, e);
        }
    }

    public HttpResponse<String> post(String url, String jsonBody) {
        return post(url, jsonBody, null);
    }

    public HttpResponse<String> post(String url, String jsonBody, String authHeader) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
        if (authHeader != null)
            builder.header("Authorization", authHeader);
        return sendString(builder.build(), url);
    }

    private HttpResponse<String> sendString(HttpRequest request, String url) {
        try {
            return CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new VcsProviderException("HTTP request failed for " + url + ": " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VcsProviderException("HTTP request interrupted for " + url, e);
        }
    }

    public static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    /** Encodes an object path for a URL, keeping URL-unsafe slashes intact. */
    public static String encodePath(String path) {
        StringBuilder sb = new StringBuilder();
        for (String part : path.split("/")) {
            if (sb.length() > 0)
                sb.append('/');
            sb.append(URLEncoder.encode(part, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // JSON (tools.jackson) helpers
    // ------------------------------------------------------------------

    public static List<JsonObj> parseArray(String jsonBody) {
        List<JsonObj> result = new ArrayList<>();
        try {
            JsonNode root = MAPPER.readTree(jsonBody);
            if (root != null && root.isArray()) {
                for (JsonNode element : root)
                    result.add(new JsonObj(element));
            }
        } catch (tools.jackson.core.JacksonException e) {
            throw new VcsProviderException("Cannot parse JSON array: " + e.getMessage(), e);
        }
        return result;
    }

    public static JsonObj parseMap(String jsonBody) {
        try {
            JsonNode root = MAPPER.readTree(jsonBody);
            return new JsonObj(root);
        } catch (tools.jackson.core.JacksonException e) {
            throw new VcsProviderException("Cannot parse JSON object: " + e.getMessage(), e);
        }
    }

    public static String toJson(Map<String, String> body) {
        try {
            return MAPPER.writeValueAsString(body);
        } catch (tools.jackson.core.JacksonException e) {
            throw new VcsProviderException("Cannot serialize JSON: " + e.getMessage(), e);
        }
    }

    public static String toJsonWithNested(Map<String, Object> body) {
        try {
            return MAPPER.writeValueAsString(body);
        } catch (tools.jackson.core.JacksonException e) {
            throw new VcsProviderException("Cannot serialize JSON: " + e.getMessage(), e);
        }
    }

    public static final class JsonObj {
        private final JsonNode node;

        public JsonObj(JsonNode node) {
            this.node = node;
        }

        public String getString(String name) {
            JsonNode value = node == null ? null : node.get(name);
            return value == null || value.isNull() ? null : value.asText();
        }
    }
}