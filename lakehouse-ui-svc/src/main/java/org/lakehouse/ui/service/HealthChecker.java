/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
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
package org.lakehouse.ui.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

@Component
public class HealthChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthChecker.class);

    private final RestClient restClient;
    private final long timeoutMs;

    public HealthChecker(RestClient.Builder builder,
                         @Value("${lakehouse.ui.health-check-timeout-ms:3000}") long timeoutMs) {
        this.timeoutMs = timeoutMs;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setReadTimeout((int) timeoutMs);
        requestFactory.setConnectTimeout((int) timeoutMs);
        this.restClient = builder.requestFactory(requestFactory).build();
    }

    public boolean isAlive(String url) {
        try {
            restClient.get().uri(url).retrieve().toBodilessEntity();
            return true;
        } catch (Exception e) {
            LOGGER.warn("Health check failed for {}: {}", url, e.getMessage());
            return false;
        }
    }

    public boolean isPortOpen(String hostPort) {
        String host = hostPort;
        int port;
        try {
            int lastColon = hostPort.lastIndexOf(':');
            if (lastColon <= 0 || lastColon == hostPort.length() - 1) {
                throw new IllegalArgumentException("Invalid host:port: " + hostPort);
            }
            host = hostPort.substring(0, lastColon);
            port = Integer.parseInt(hostPort.substring(lastColon + 1));
        } catch (RuntimeException e) {
            LOGGER.warn("Invalid host:port {}: {}", hostPort, e.getMessage());
            return false;
        }

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), (int) timeoutMs);
            return true;
        } catch (IOException e) {
            LOGGER.warn("Port check failed for {}: {}", hostPort, e.getMessage());
            return false;
        }
    }
}
