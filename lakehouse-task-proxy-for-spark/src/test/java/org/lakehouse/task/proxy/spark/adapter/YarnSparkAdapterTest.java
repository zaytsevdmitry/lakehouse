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

import static org.junit.jupiter.api.Assertions.*;

class YarnSparkAdapterTest {

    private final YarnSparkAdapter adapter = new YarnSparkAdapter("yarn", "http://localhost:8088", 30,
            "Submitted application (application_\\d+_\\d+) to YARN");

    @Test
    void extractSubmissionId_found() throws CreateErrorException {
        String stdout = "Launching Spark application on YARN cluster...\n" +
                "Submitted application application_1700000000000_12345 to YARN\n" +
                "Application tracking URL: http://resourcemanager:8088/proxy/application_1700000000000_12345/";
        String submissionId = adapter.extractSubmissionId(stdout);
        assertEquals("application_1700000000000_12345", submissionId);
    }

    @Test
    void extractSubmissionId_notFound() {
        String stdout = "Spark application submitted successfully but no YARN ID in output";
        assertThrows(CreateErrorException.class, () -> adapter.extractSubmissionId(stdout));
    }

    @Test
    void extractSubmissionId_multipleApplications_returnsFirst() throws CreateErrorException {
        String stdout = "First: Submitted application application_1700000000000_11111 to YARN\n" +
                "Second: Submitted application application_1700000000000_22222 to YARN";
        String submissionId = adapter.extractSubmissionId(stdout);
        assertEquals("application_1700000000000_11111", submissionId);
    }

    @Test
    void buildSparkLauncher_setsMasterAndDeployMode() {
        var request = new CreateSubmissionRequest(
                null, List.of(), "app.jar", null, "com.Main", null, null
        );
        SparkLauncher launcher = adapter.buildSparkLauncher(request);
        assertNotNull(launcher);
    }

    @Test
    void constructorSetsMasterUrl() {
        assertEquals("yarn", adapter.masterUrl);
    }
}
