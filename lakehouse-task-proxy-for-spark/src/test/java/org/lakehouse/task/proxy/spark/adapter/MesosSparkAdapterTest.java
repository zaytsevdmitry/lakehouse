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
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;

import static org.junit.jupiter.api.Assertions.*;

class MesosSparkAdapterTest {

    private final MesosSparkAdapter adapter = new MesosSparkAdapter("mesos://master:5050", 30);

    @Test
    void extractSubmissionId_alwaysThrows() {
        assertThrows(CreateErrorException.class, () -> adapter.extractSubmissionId("any output"));
    }

    @Test
    void createSubmission_throwsUnsupported() {
        CreateSubmissionRequest request = new CreateSubmissionRequest(null, null, null, null, null, null, null);
        assertThrows(UnsupportedOperationException.class, () -> adapter.createSubmission(request));
    }

    @Test
    void constructorSetsMasterUrl() {
        assertEquals("mesos://master:5050", adapter.masterUrl);
    }

    @Test
    void killSubmission_returnsNotImplemented() {
        var result = adapter.killSubmission("some-id");
        assertFalse(result.success());
        assertEquals("MESOS adapter not implemented", result.message());
    }

    @Test
    void getSubmissionStatus_returnsNotImplemented() {
        var result = adapter.getSubmissionStatus("some-id");
        assertEquals("MESOS adapter not implemented", result.message());
    }
}
