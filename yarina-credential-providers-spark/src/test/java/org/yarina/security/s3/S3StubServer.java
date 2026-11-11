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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minimal in-process stub of an S3 / MinIO endpoint used by the S3A integration test.
 *
 * <p>Every object request (HEAD or GET without query parameters) is answered with
 * HTTP 404 and an S3 error XML body, and every listing request (GET with query
 * parameters, e.g. {@code list-type=2}) is answered with HTTP 200 and an empty
 * {@code ListBucketResult}. The net effect for {@code S3AFileSystem#getFileStatus}:
 * HEAD misses, the listing is empty, the path is reported as not existing
 * ({@code FileNotFoundException}) — without any of the S3 data actually existing.
 *
 * <p>The purpose of the stub is only to force the AWS SDK to <b>sign</b> requests:
 * signing makes S3A ask its {@code AWSCredentialsProvider} (the class under test)
 * for credentials, which is exactly what the integration test measures.
 */
final class S3StubServer implements Closeable {

    private static final String ERROR_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<Error>"
            + "<Code>NoSuchKey</Code>"
            + "<Message>The specified key does not exist.</Message>"
            + "<RequestId>stub-request</RequestId>"
            + "<HostId>stub-host</HostId>"
            + "</Error>";

    private static final String EMPTY_LIST_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<ListBucketResult>"
            + "<Name>stub-bucket</Name>"
            + "<Prefix></Prefix>"
            + "<KeyCount>0</KeyCount>"
            + "<MaxKeys>1000</MaxKeys>"
            + "<IsTruncated>false</IsTruncated>"
            + "</ListBucketResult>";

    private final HttpServer server;
    private final ExecutorService executor;
    private final AtomicInteger requests = new AtomicInteger();

    private S3StubServer() throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        this.executor = Executors.newFixedThreadPool(8);
        server.setExecutor(executor);
        server.createContext("/", this::handle);
        server.start();
    }

    static S3StubServer start() throws IOException {
        return new S3StubServer();
    }

    /** Endpoint URL, e.g. {@code http://127.0.0.1:54321}. */
    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /** Total number of received S3 requests (objects + listings). */
    int requests() {
        return requests.get();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    /* ------------------------------------------------------------------ */

    private void handle(HttpExchange exchange) throws IOException {
        try {
            requests.incrementAndGet();
            boolean isListing = exchange.getRequestURI().getRawQuery() != null;
            if (isListing) {
                respond(exchange, 200, EMPTY_LIST_XML);
            } else {
                respond(exchange, 404, ERROR_XML);
            }
        } finally {
            exchange.close();
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        if ("HEAD".equalsIgnoreCase(exchange.getRequestMethod())) {
            // A HEAD response must not carry a body; terminate with an empty chunked body.
            exchange.getResponseHeaders().set("Content-Type", "application/xml");
            exchange.sendResponseHeaders(status, -1);
            exchange.getResponseBody().close();
            return;
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/xml");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
