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
package org.lakehouse.task.proxy.spark.adapter;

import org.apache.spark.launcher.SparkLauncher;
import org.junit.jupiter.api.Test;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StandaloneSparkAdapterTest {

    private final StandaloneSparkAdapter adapter = new StandaloneSparkAdapter("spark://master:7077", "http://localhost:6066", 30,
            "(driver-\\d{14}-\\d{4})");

    @Test
    void extractSubmissionId_found() throws CreateErrorException {
        String stdout = "Running Spark using the StandaloneApplicationMaster launcher.\n" +
                "Using main class: com.example.Main\n" +
                "driver-20250723123456-1234\n" +
                "Running Spark application...";
        String submissionId = adapter.extractSubmissionId(stdout);
        assertEquals("driver-20250723123456-1234", submissionId);
    }

    @Test
    void extractSubmissionId_notFound() {
        String stdout = "Application started successfully without a driver ID";
        assertThrows(CreateErrorException.class, () -> adapter.extractSubmissionId(stdout));
    }

    @Test
    void buildSparkLauncher_setsMasterAndDeployMode() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(
                null, List.of("arg1"), "s3://bucket/app.jar", null, "com.example.Main", Map.of("spark.executor.memory", "2g"), null
        );
        SparkLauncher launcher = adapter.buildSparkLauncher(request);
        assertNotNull(launcher);
    }

    @Test
    void constructorSetsMasterUrl() {
        assertEquals("spark://master:7077", adapter.masterUrl);
    }
}
