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

package org.lakehouse.client.rest.taskproxy;

import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionPropertiesDTO;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionsRequest;
import org.lakehouse.task.proxy.spark.dto.SparkProxySubmissionsResponse;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;

public interface SparkProxyRestClientApi {

    SubmissionResponse createSubmission(CreateSubmissionRequest request);

    SubmissionStatusResponse getStatus(Long submissionId);

    SparkProxySubmissionsResponse getSubmissions(SparkProxySubmissionsRequest request);

    SparkProxySubmissionPropertiesDTO getSparkProperties(Long id);

    SubmissionResponse killSubmission(Long submissionId);

    SubmissionResponse killAllSubmissions();

    SubmissionResponse clearCompleted();
}
