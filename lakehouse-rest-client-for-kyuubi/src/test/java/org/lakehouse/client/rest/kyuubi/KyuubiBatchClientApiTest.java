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
package org.lakehouse.client.rest.kyuubi; // Match your project's test package structure

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class KyuubiBatchClientApiTest {

    private ClientAndServer mockServer;
    private KyuubiBatchClientApi client;
    private static final int PORT = 10099;
    private static final String BASE_URL = "http://localhost:" + PORT;
    
    // Expected "Basic YWRtaW46cGFzc3dvcmQ=" for username="admin", password="password"
    private static final String EXPECTED_AUTH_HEADER = "Basic YWRtaW46cGFzc3dvcmQ=";

    @BeforeEach
    void setUp() {
        // Start mock HTTP server before each test
        mockServer = startClientAndServer(PORT);

        // Initialize the real factory to get configured HttpClient and ObjectMapper
        KyuubiBatchClientFactory factory = new KyuubiBatchClientFactory();
        client = factory.createClient(BASE_URL, "admin", "password");
    }

    @AfterEach
    void tearDown() {
        // Stop mock server after each test to release the port
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @Test
    void testCreateBatch_Success() throws Exception {
        // Given
        BatchRequest requestBody = new BatchRequest("Spark", "hdfs:///app.jar", "com.Main", new ArrayList<>(),Map.of("spark.master", "yarn"));
        requestBody.setName("Test-Job");
        requestBody.setArgs(List.of("arg1"));

        String mockJsonResponse = "{\n" +
                "  \"id\": \"batch-uuid-123\",\n" +
                "  \"user\": \"admin\",\n" +
                "  \"batchType\": \"Spark\",\n" +
                "  \"state\": \"PENDING\"\n" +
                "}";

        // Expectation setup for MockServer
        mockServer.when(
                request()
                        .withMethod("POST")
                        .withPath("/api/v1/batches")
                        .withHeader("Authorization", EXPECTED_AUTH_HEADER)
                        .withHeader("Content-Type", "application/json")
                        .withBody(org.mockserver.model.JsonBody.json("{\n" +
                                "  \"batchType\": \"Spark\",\n" +
                                "  \"resource\": \"hdfs:///app.jar\",\n" +
                                "  \"className\": \"com.Main\",\n" +
                                "  \"name\": \"Test-Job\",\n" +
                                "  \"args\": [\"arg1\"],\n" +
                                "  \"configs\": {\"spark.master\":\"yarn\"}\n" +
                                "}"))
        ).respond(
                response()
                        .withStatusCode(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockJsonResponse)
        );

        // When
        BatchResponse response = client.createBatch(requestBody);

        // Then
        assertNotNull(response);
        assertEquals("batch-uuid-123", response.getId());
        assertEquals("PENDING", response.getState());
        assertEquals("Spark", response.getBatchType());
    }

    @Test
    void testGetBatchStatus_Success() throws Exception {
        // Given
        String batchId = "batch-uuid-123";
        String mockJsonResponse = "{\n" +
                "  \"id\": \"batch-uuid-123\",\n" +
                "  \"state\": \"RUNNING\",\n" +
                "  \"appId\": \"application_123456789_0001\"\n" +
                "}";

        mockServer.when(
                request()
                        .withMethod("GET")
                        .withPath("/api/v1/batches/" + batchId)
                        .withHeader("Authorization", EXPECTED_AUTH_HEADER)
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockJsonResponse)
        );

        // When
        BatchResponse response = client.getBatchStatus(batchId);

        // Then
        assertNotNull(response);
        assertEquals("batch-uuid-123", response.getId());
        assertEquals("RUNNING", response.getState());
        assertNotNull(response.getAppId());
        assertEquals("application_123456789_0001", response.getAppId());
    }

    @Test
    void testCancelBatch_Success() throws Exception {
        // Given
        String batchId = "batch-uuid-123";

        mockServer.when(
                request()
                        .withMethod("DELETE")
                        .withPath("/api/v1/batches/" + batchId)
                        .withHeader("Authorization", EXPECTED_AUTH_HEADER)
        ).respond(
                response()
                        .withStatusCode(200)
        );

        // When & Then
        assertDoesNotThrow(() -> client.cancelBatch(batchId));
    }

    @Test
    void testGetBatchStatus_Failure_ThrowsRuntimeException() {
        // Given
        String batchId = "invalid-id";

        mockServer.when(
                request()
                        .withMethod("GET")
                        .withPath("/api/v1/batches/" + batchId)
        ).respond(
                response()
                        .withStatusCode(404)
                        .withBody("Batch not found")
        );

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> client.getBatchStatus(batchId));
        assertTrue(exception.getMessage().contains("Failed to get batch status. Status: 404"));
    }
}
