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

import org.junit.jupiter.api.Test;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;

import static org.junit.jupiter.api.Assertions.*;

class KubernetesSparkAdapterTest {

    @Test
    void extractSubmissionId_driverPodNamePattern() throws CreateErrorException {
        KubernetesSparkAdapter k8sAdapter = new KubernetesSparkAdapter("k8s://host:443", null, "default", 30,
                "(?:driver\\s+)?pod name:\\s+([a-zA-Z0-9\\-]+-driver)");
        String stdout = "INFO KubernetesClusterSubmissionClient: driver pod name: spark-analytics-job-123456-driver\n" +
                "Successfully submitted";
        String submissionId = k8sAdapter.extractSubmissionId(stdout);
        assertEquals("spark-analytics-job-123456-driver", submissionId);
    }

    @Test
    void extractSubmissionId_driverPodNamePatternWithHyphens() throws CreateErrorException {
        KubernetesSparkAdapter k8sAdapter = new KubernetesSparkAdapter("k8s://host:443", null, "default", 30,
                "(?:driver\\s+)?pod name:\\s+([a-zA-Z0-9\\-]+-driver)");
        String stdout = "Submitted application driver pod name: 18-0-regular-transaction-dds-prepare-20250102T000000Z-driver\n" +
                "Pod created successfully";
        String submissionId = k8sAdapter.extractSubmissionId(stdout);
        assertEquals("18-0-regular-transaction-dds-prepare-20250102T000000Z-driver", submissionId);
    }

    @Test
    void extractSubmissionId_notFound() {
        KubernetesSparkAdapter k8sAdapter = new KubernetesSparkAdapter("k8s://host:443", null, "default", 30,
                "(?:driver\\s+)?pod name:\\s+([a-zA-Z0-9\\-]+-driver)");
        String stdout = "No submission info in this log line";
        assertThrows(CreateErrorException.class, () -> k8sAdapter.extractSubmissionId(stdout));
    }

    @Test
    void extractSubmissionId_emptyOutput() {
        KubernetesSparkAdapter k8sAdapter = new KubernetesSparkAdapter("k8s://host:443", null, "default", 30,
                "(?:driver\\s+)?pod name:\\s+([a-zA-Z0-9\\-]+-driver)");
        assertThrows(CreateErrorException.class, () -> k8sAdapter.extractSubmissionId(""));
    }

    @Test
    void constructorSetsMasterUrl() {
        KubernetesSparkAdapter k8sAdapter = new KubernetesSparkAdapter("k8s://host:443", null, "default", 30,
                "(?:driver\\s+)?pod name:\\s+([a-zA-Z0-9\\-]+-driver)");
        assertEquals("k8s://host:443", k8sAdapter.masterUrl);
    }
}
