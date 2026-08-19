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

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.ExternalStatus;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KubernetesSparkAdapter extends SparkAdapterBase {
    private  final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final String LABEL_APP = "spark-app";
    private static final String DEFAULT_NAMESPACE = "default";
    private final Pattern k8sDriverNamePattern;

    private final CoreV1Api coreV1Api;
    private final String defaultNamespace;

    public KubernetesSparkAdapter(String masterUrl, CoreV1Api coreV1Api, String defaultNamespace, long submissionTimeoutSeconds, String submissionIdPattern) {
        super(masterUrl, submissionTimeoutSeconds);
        this.coreV1Api = coreV1Api;
        this.defaultNamespace = defaultNamespace;
        this.k8sDriverNamePattern = Pattern.compile(submissionIdPattern, Pattern.CASE_INSENSITIVE);
        log.info("Initialised SparkAdapter {} with masterUrl: {}, namespace {}",
                KubernetesSparkAdapter.class.getSimpleName(),
                masterUrl,
                defaultNamespace );
    }

    @Override
    protected String extractSubmissionId(String output) throws CreateErrorException {
        Matcher driverNameMatcher = k8sDriverNamePattern.matcher(output);
        if (driverNameMatcher.find()) {
            return driverNameMatcher.group(1);
        }
        String snippet = output.length() > 2000 ? output.substring(0, 2000) + "..." : output;
        log.error("K8s driver pod name not found in spark-submit output. Output:\n{}", snippet);
        throw new CreateErrorException("K8s driver pod name not found in output. Output length: " + output.length());
    }

    private String resolveNamespace() {
        if (defaultNamespace != null && !defaultNamespace.isBlank()) {
            return defaultNamespace.trim();
        }
        log.info("Namespace in configuration is empty, using default");
        return DEFAULT_NAMESPACE;
    }

    @Override
    public String createSubmission(CreateSubmissionRequest request) throws CreateErrorException {
        if (request.sparkProperties() != null) {
            log.info("spark.app.name = {}", request.sparkProperties().get("spark.app.name"));
        }
        return defaultCreateSubmission(request);
    }

    @Override
    public SubmissionResponse killSubmission(String submissionId) {
        String namespace = resolveNamespace();
        String podName = findDriverPodName(submissionId, namespace);
        if (podName == null) {
            return new SubmissionResponse("KillResponse", ExternalStatus.UNKNOWN.name(), null, submissionId, false);
        }
        try {
            coreV1Api.deleteNamespacedPod(podName, namespace).execute();
            log.info("Deleted driver pod {} for submission {} in namespace {}", podName, submissionId, namespace);
            return new SubmissionResponse("KillResponse", ExternalStatus.KILLED.name(), null, submissionId, true);
        } catch (ApiException e) {
            log.error("Failed to delete pod {}: {}", podName, e.getResponseBody(), e);
            return new SubmissionResponse("KillResponse", ExternalStatus.FAILED.name(), null, submissionId, false);
        }
    }

    @Override
    public SubmissionResponse killAllSubmissions() {
        String namespace = resolveNamespace();
        try {
            V1PodList pods = coreV1Api.listNamespacedPod(namespace)
                    .labelSelector(LABEL_APP + "=spark-driver")
                    .execute();
            int deleted = 0;
            if (pods.getItems() != null) {
                for (V1Pod pod : pods.getItems()) {
                    String name = pod.getMetadata().getName();
                    coreV1Api.deleteNamespacedPod(name, namespace).execute();
                    deleted++;
                }
            }
            log.warn("Deleted {} driver pods in namespace {}", deleted, namespace);
            return new SubmissionResponse("KillAllResponse", ExternalStatus.KILLED.name() + " " + deleted + " pods", null, null, true);
        } catch (ApiException e) {
            log.error("Failed to kill all: {}", e.getResponseBody(), e);
            return new SubmissionResponse("KillAllResponse", ExternalStatus.FAILED.name(), null, null, false);
        }
    }

    @Override
    public SubmissionStatusResponse getSubmissionStatus(String submissionId) {
        String namespace = resolveNamespace();
        String podName = findDriverPodName(submissionId, namespace);
        if (podName == null) {
            return new SubmissionStatusResponse(
                    "SparkStatusResponse",
                    ExternalStatus.WAITING.name(),
                    null,
                    submissionId, true,
                    ExternalStatus.WAITING.name(),
                    null,
                    null);
        }
        try {
            V1Pod pod = coreV1Api.readNamespacedPod(podName, namespace).execute();
            String phase = pod.getStatus() != null ? pod.getStatus().getPhase() : null;
            ExternalStatus external = ExternalStatus.fromK8sPhase(phase);
            boolean success = external == ExternalStatus.RUNNING || external == ExternalStatus.FINISHED;
            log.debug("Status for submission {}: pod={}, phase={} -> {} in namespace {}", submissionId, podName, phase, external, namespace);
            return new SubmissionStatusResponse(
                    "SparkStatusResponse",
                    phase,
                    null,
                    submissionId,
                    success,
                    external.name(),
                    null,
                    null);
        } catch (ApiException e) {
            log.error("Failed to get status for pod {}: {}", podName, e.getResponseBody(), e);
            return new SubmissionStatusResponse("SparkStatusResponse", e.getMessage(), null, submissionId, false, ExternalStatus.UNKNOWN.name(), null, null);
        }
    }

    @Override
    public SubmissionResponse clearCompleted(String submissionId) {
        String namespace = resolveNamespace();
        String podName = findDriverPodName(submissionId, namespace);
        if (podName == null) {
            log.warn("K8s driver pod for submission {} not found during clear in namespace {}", submissionId, namespace);
            return new SubmissionResponse("ClearResponse", "NOT_FOUND", null, submissionId, true);
        }
        try {
            coreV1Api.deleteNamespacedPod(podName, namespace).execute();
            log.info("Cleared K8s driver pod {} for submission {} in namespace {}", podName, submissionId, namespace);
            return new SubmissionResponse("ClearResponse", ExternalStatus.FINISHED.name(), null, submissionId, true);
        } catch (ApiException e) {
            log.error("Failed to clear K8s pod {} for submission {}: {}", podName, submissionId, e.getResponseBody(), e);
            return new SubmissionResponse("ClearResponse", ExternalStatus.FAILED.name(), null, submissionId, false);
        }
    }

    private String findDriverPodName(String submissionId, String namespace) {
        if (submissionId == null || submissionId.isBlank()) return null;
        try {
            coreV1Api.readNamespacedPod(submissionId, namespace).execute();
            return submissionId;
        } catch (ApiException e) {
            log.debug("Driver pod {} not found in namespace {}: {}", submissionId, namespace, e.getResponseBody());
            return null;
        }
    }
}
