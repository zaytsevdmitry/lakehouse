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

import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MesosSparkAdapter extends SparkAdapterBase {
    private  final Logger log = LoggerFactory.getLogger(this.getClass());
    public MesosSparkAdapter(String masterUrl, long submissionTimeoutSeconds) {
        super(masterUrl, submissionTimeoutSeconds);
        log.info("Initialised SparkAdapter {} with masterUrl: {}",
                KubernetesSparkAdapter.class.getSimpleName(),
                masterUrl);
    }

    @Override
    protected String extractSubmissionId(String output) throws CreateErrorException {
        throw new CreateErrorException("MESOS adapter not implemented");
    }

    @Override
    public String createSubmission(CreateSubmissionRequest request) throws CreateErrorException {
        throw new UnsupportedOperationException("MESOS adapter not implemented");
    }

    @Override
    public SubmissionResponse killSubmission(String submissionId) {
        log.warn("MESOS killSubmission not yet implemented. submissionId={}", submissionId);
        return new SubmissionResponse("KillResponse", "MESOS adapter not implemented", null, submissionId, false);
    }

    @Override
    public SubmissionResponse killAllSubmissions() {
        log.warn("MESOS killAllSubmissions not yet implemented");
        return new SubmissionResponse("KillAllResponse", "MESOS adapter not implemented", null, null, false);
    }

    @Override
    public SubmissionStatusResponse getSubmissionStatus(String submissionId) {
        log.warn("MESOS getSubmissionStatus not yet implemented. submissionId={}", submissionId);
        return new SubmissionStatusResponse("SparkStatusResponse", "MESOS adapter not implemented", null, submissionId, false, null, null, null);
    }
}
