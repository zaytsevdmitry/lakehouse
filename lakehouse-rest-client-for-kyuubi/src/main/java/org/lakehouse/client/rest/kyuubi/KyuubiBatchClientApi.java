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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class KyuubiBatchClientApi {

    private final String baseUrl;
    private final String authHeader;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Kyuubi API Client Constructor with Dependency Injection
     * @param baseUrl Kyuubi server base URL (e.g., "http://localhost:10099")
     * @param username Username for Basic Auth
     * @param password Password for Basic Auth
     * @param httpClient Shared or custom configured HttpClient instance
     * @param objectMapper Shared or custom configured ObjectMapper instance
     */
    public KyuubiBatchClientApi(String baseUrl, String username, String password, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        
        // Generate Basic Auth header
        String auth = username + ":" + password;
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 1. Create a new Batch Job
     */
    public BatchResponse createBatch(BatchRequest request) throws IOException, InterruptedException {
        String requestBody = objectMapper.writeValueAsString(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/batches"))
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200 && response.statusCode() != 201) {
            throw new RuntimeException("Failed to create batch. Status: " + response.statusCode() + ", Body: " + response.body());
        }

        return objectMapper.readValue(response.body(), BatchResponse.class);
    }

    /**
     * 2. Retrieve Batch Job Status by ID
     */
    public BatchResponse getBatchStatus(String batchId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/batches/" + batchId))
                .header("Authorization", authHeader)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get batch status. Status: " + response.statusCode() + ", Body: " + response.body());
        }

        return objectMapper.readValue(response.body(), BatchResponse.class);
    }

    /**
     * 3. Terminate (Cancel/Delete) a Batch Job
     */
    public void cancelBatch(String batchId) throws IOException, InterruptedException {
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/batches/" + batchId))
                .header("Authorization", authHeader)
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to cancel batch. Status: " + response.statusCode() + ", Body: " + response.body());
        }
        System.out.println("Batch " + batchId + " successfully canceled.");
    }
}
