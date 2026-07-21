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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

class KyuubiBatchPollingTest {

    private ClientAndServer mockServer;
    private KyuubiBatchClientApi client;
    private static final int PORT = 10099;

    @BeforeEach
    void setUp() {
        mockServer = startClientAndServer(PORT);
        KyuubiBatchClientFactory factory = new KyuubiBatchClientFactory();
        client = factory.createClient("http://localhost:" + PORT, "admin", "password");
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @Test
    void testBatchStatusTransition_WithAwaitilityPolling() {
        String batchId = "batch-123";
        String path = "/api/v1/batches/" + batchId;

        // 1. First request returns PENDING (exactly 1 time)
        mockServer.when(
                request().withMethod("GET").withPath(path),
                Times.exactly(1)
        ).respond(
                response().withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"batch-123\", \"state\":\"PENDING\"}")
        );

        // 2. Next 2 requests return RUNNING (exactly 2 times)
        mockServer.when(
                request().withMethod("GET").withPath(path),
                Times.exactly(2)
        ).respond(
                response().withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"batch-123\", \"state\":\"RUNNING\"}")
        );

        // 3. Any subsequent requests return FINISHED (unlimited)
        mockServer.when(
                request().withMethod("GET").withPath(path),
                Times.unlimited()
        ).respond(
                response().withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"batch-123\", \"state\":\"FINISHED\"}")
        );

        // --- CLIENT-SIDE POLLING LOOP VIA AWAITILITY ---
        
        // Counter to track states in our assertions
        AtomicInteger pollCount = new AtomicInteger(0);

        // Awaitility will poll the lambda expression until it returns true or times out
        await()
            .atMost(Duration.ofSeconds(10))     // Maximum time to wait before failing the test
            .pollInterval(Duration.ofMillis(500)) // How often to call the API (poll interval)
            .until(() -> {
                int currentPoll = pollCount.incrementAndGet();
                
                // Perform the actual HTTP call via our API client
                BatchResponse response = client.getBatchStatus(batchId);
                System.out.println("Poll #" + currentPoll + " -> Current state detected: " + response.getState());

                // Inline assertions to verify the state machine progresses properly
                if (currentPoll == 1) {
                    assertEquals("PENDING", response.getState());
                } else if (currentPoll == 2 || currentPoll == 3) {
                    assertEquals("RUNNING", response.getState());
                } else {
                    assertEquals("FINISHED", response.getState());
                }

                // Return true when the terminal state is reached to break the polling loop
                return "FINISHED".equals(response.getState());
            });

        System.out.println("Success! Polling loop finished seamlessly.");
    }
}
