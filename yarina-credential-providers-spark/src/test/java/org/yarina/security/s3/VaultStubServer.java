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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal in-process stub of the OpenBao / Vault HTTP API used by tests.
 *
 * <p>Emulates the two endpoints used by {@code VaultHttp}:
 * <ul>
 *   <li>{@code POST /v1/auth/<mount>/login} — Kubernetes auth; every login issues a fresh
 *       token ({@code stub-token-<n>}); the request bodies are captured so tests can
 *       assert the role passed by the client;</li>
 *   <li>{@code GET /v1/<secretPath>} — KV v2 read; validates the {@code X-Vault-Token}
 *       header against the currently valid token and returns the configured payload
 *       ({@code {"data":{"data":{...}}}}).</li>
 * </ul>
 *
 * <p>Two modes:
 * <ul>
 *   <li>{@link #start()} — auto-rotate: each login sets the newly issued token as the
 *       valid one, so a 403-triggered re-auth recovers;</li>
 *   <li>{@link #startWithFixedToken(String)} — the valid token never changes, so a
 *       re-authenticated client keeps receiving 403 (used to assert the single retry).</li>
 * </ul>
 */
final class VaultStubServer implements Closeable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final HttpServer server;
    private final ExecutorService executor;

    private final AtomicInteger reads = new AtomicInteger();
    private final AtomicInteger logins = new AtomicInteger();
    private final List<String> loginBodies = new CopyOnWriteArrayList<>();

    /** Payloads keyed by secret path (without the /v1/ prefix). */
    private final Map<String, Map<String, String>> payloadsByPath = new ConcurrentHashMap<>();
    private final Map<String, String> defaultPayload = new ConcurrentHashMap<>();

    private volatile String validToken;
    private volatile boolean failReads;
    private final boolean rotateTokensOnLogin;

    private VaultStubServer(String fixedToken, boolean rotateTokensOnLogin) throws IOException {
        this.rotateTokensOnLogin = rotateTokensOnLogin;
        this.validToken = fixedToken;
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.executor = Executors.newFixedThreadPool(4);
        server.setExecutor(executor);
        server.createContext("/v1/auth/", this::handleLogin);
        server.createContext("/v1/", this::handleRead);
        server.start();
    }

    /** Auto-rotate mode: logins issue and activate fresh tokens. */
    static VaultStubServer start() throws IOException {
        return new VaultStubServer(null, true);
    }

    /** Fixed-token mode: only {@code fixedToken} is accepted; logins do not activate tokens. */
    static VaultStubServer startWithFixedToken(String fixedToken) throws IOException {
        return new VaultStubServer(fixedToken, false);
    }

    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    int reads() {
        return reads.get();
    }

    int logins() {
        return logins.get();
    }

    List<String> loginBodies() {
        return Collections.unmodifiableList(loginBodies);
    }

    /** Payload returned for every secret path. */
    void setPayload(Map<String, String> kv) {
        defaultPayload.clear();
        defaultPayload.putAll(kv);
    }

    /** Payload returned for one specific secret path (takes priority over the default). */
    void setPayload(String secretPath, Map<String, String> kv) {
        payloadsByPath.put(secretPath, new HashMap<>(kv));
    }

    /** Make the current token invalid: reads return 403 until the next login. */
    void expireToken() {
        validToken = null;
    }

    /** Make every read return HTTP 500. */
    void setFailReads(boolean failReads) {
        this.failReads = failReads;
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    /* ------------------------------------------------------------------ */

    private void handleLogin(HttpExchange exchange) throws IOException {
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            loginBodies.add(body);
            int seq = logins.incrementAndGet();
            String issuedToken = "stub-token-" + seq;
            if (rotateTokensOnLogin) {
                validToken = issuedToken;
            }
            respond(exchange, 200, MAPPER.writeValueAsString(
                    Map.of("auth", Map.of("client_token", issuedToken))));
        } finally {
            exchange.close();
        }
    }

    private void handleRead(HttpExchange exchange) throws IOException {
        try {
            reads.incrementAndGet();
            if (failReads) {
                respond(exchange, 500, "{\"errors\":[\"internal\"]}");
                return;
            }
            String presentedToken = exchange.getRequestHeaders().getFirst("X-Vault-Token");
            if (validToken == null || !validToken.equals(presentedToken)) {
                respond(exchange, 403, "{\"errors\":[\"permission denied\"]}");
                return;
            }
            String secretPath = exchange.getRequestURI().getPath().substring("/v1/".length());
            Map<String, String> payload = payloadsByPath.getOrDefault(secretPath, defaultPayload);
            respond(exchange, 200, MAPPER.writeValueAsString(
                    Map.of("data", Map.of("data", new HashMap<>(payload)))));
        } finally {
            exchange.close();
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
