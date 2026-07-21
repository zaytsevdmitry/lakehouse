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
package org.lakehouse.client.rest.kyuubi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.time.Duration;

public class KyuubiBatchClientFactory {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a factory using default safe configurations for HTTP and JSON processing.
     * This shares one HttpClient instance for all generated clients to optimize connection pooling.
     */
    public KyuubiBatchClientFactory() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * Creates a factory injecting custom pre-configured instances.
     */
    public KyuubiBatchClientFactory(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a new KyuubiBatchClientApi instance with the factory's shared HTTP/JSON configurations.
     * 
     * @param baseUrl Target Kyuubi server endpoint
     * @param username Basic auth username
     * @param password Basic auth password
     * @return Fully configured KyuubiBatchClientApi instance
     */
    public KyuubiBatchClientApi createClient(String baseUrl, String username, String password) {
        return new KyuubiBatchClientApi(baseUrl, username, password, this.httpClient, this.objectMapper);
    }
}
