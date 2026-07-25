package org.lakehouse.task.proxy.spark.adapter;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.ExternalStatus;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.exception.CreateErrorException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KubernetesSparkAdapter extends SparkAdapterBase {

    private static final String LABEL_APP = "spark-app";
    private static final String DEFAULT_NAMESPACE = "default";
    private static final Pattern K8S_POD_PATTERN = Pattern.compile("pods/(spark-\\S+)");
    private static final Pattern K8S_DRIVER_PATTERN = Pattern.compile("driver-\\S+");

    private final CoreV1Api coreV1Api;
    private final String defaultNamespace;

    public KubernetesSparkAdapter(String masterUrl, CoreV1Api coreV1Api, String defaultNamespace) {
        super(masterUrl);
        this.coreV1Api = coreV1Api;
        this.defaultNamespace = defaultNamespace;
    }

    @Override
    protected String extractSubmissionId(String output) throws CreateErrorException {
        Matcher podMatcher = K8S_POD_PATTERN.matcher(output);
        if (podMatcher.find()) {
            return podMatcher.group(1);
        }
        Matcher driverMatcher = K8S_DRIVER_PATTERN.matcher(output);
        if (driverMatcher.find()) {
            return driverMatcher.group(0);
        }
        throw new CreateErrorException("K8s driver pod name not found in output");
    }

    private String resolveNamespace() {
        if (defaultNamespace != null && !defaultNamespace.isBlank()) {
            return defaultNamespace.trim();
        }
        return DEFAULT_NAMESPACE;
    }

    @Override
    public String createSubmission(CreateSubmissionRequest request) throws CreateErrorException {
        return defaultCreateSubmission(request);
    }

    @Override
    public CreateSubmissionResponse killSubmission(String submissionId) {
        String namespace = resolveNamespace();
        String podName = findDriverPodName(submissionId, namespace);
        if (podName == null) {
            return new CreateSubmissionResponse("KillResponse", ExternalStatus.UNKNOWN.name(), null, submissionId, false);
        }
        try {
            coreV1Api.deleteNamespacedPod(podName, namespace).execute();
            log.info("Deleted driver pod {} for submission {} in namespace {}", podName, submissionId, namespace);
            return new CreateSubmissionResponse("KillResponse", ExternalStatus.KILLED.name(), null, submissionId, true);
        } catch (ApiException e) {
            log.error("Failed to delete pod {}: {}", podName, e.getResponseBody(), e);
            return new CreateSubmissionResponse("KillResponse", ExternalStatus.FAILED.name(), null, submissionId, false);
        }
    }

    @Override
    public CreateSubmissionResponse killAllSubmissions() {
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
            return new CreateSubmissionResponse("KillAllResponse", ExternalStatus.KILLED.name() + " " + deleted + " pods", null, null, true);
        } catch (ApiException e) {
            log.error("Failed to kill all: {}", e.getResponseBody(), e);
            return new CreateSubmissionResponse("KillAllResponse", ExternalStatus.FAILED.name(), null, null, false);
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
    public CreateSubmissionResponse clearCompleted() {
        String namespace = resolveNamespace();
        try {
            V1PodList pods = coreV1Api.listNamespacedPod(namespace)
                    .labelSelector(LABEL_APP + "=spark-driver")
                    .execute();
            int cleared = 0;
            if (pods.getItems() != null) {
                for (V1Pod pod : pods.getItems()) {
                    String phase = pod.getStatus() != null ? pod.getStatus().getPhase() : null;
                    ExternalStatus external = ExternalStatus.fromK8sPhase(phase);
                    if (external == ExternalStatus.FINISHED || external == ExternalStatus.FAILED) {
                        coreV1Api.deleteNamespacedPod(pod.getMetadata().getName(), namespace).execute();
                        cleared++;
                    }
                }
            }
            log.info("Cleared {} completed pods in namespace {}", cleared, namespace);
            return new CreateSubmissionResponse("ClearResponse", "Cleared " + cleared + " pods", null, null, true);
        } catch (ApiException e) {
            log.error("Failed to clear: {}", e.getResponseBody(), e);
            return new CreateSubmissionResponse("ClearResponse", ExternalStatus.FAILED.name(), null, null, false);
        }
    }

    private String findDriverPodName(String submissionId, String namespace) {
        try {
            String labelSelector = LABEL_APP + "=spark-driver,spark-submission-id=" + submissionId;
            V1PodList pods = coreV1Api.listNamespacedPod(namespace)
                    .labelSelector(labelSelector)
                    .execute();
            if (pods.getItems() != null && !pods.getItems().isEmpty()) {
                return pods.getItems().get(0).getMetadata().getName();
            }
        } catch (ApiException e) {
            log.error("Failed to find driver pod for {} in namespace {}: {}", submissionId, namespace, e.getResponseBody(), e);
        }
        return null;
    }
}
