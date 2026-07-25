package org.lakehouse.task.proxy.spark.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.lakehouse.task.proxy.spark.adapter.SparkAdapter;
import org.lakehouse.task.proxy.spark.config.ProxyConfig;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.ExternalStatus;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.entity.SparkSubmission;
import org.lakehouse.task.proxy.spark.repository.SparkSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SparkProxyService {

    private static final Logger log = LoggerFactory.getLogger(SparkProxyService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SparkSubmissionRepository repository;
    private final SparkAdapter adapter;
    private final String clusterType;

    public SparkProxyService(SparkSubmissionRepository repository, SparkAdapter adapter, ProxyConfig config) {
        this.repository = repository;
        this.adapter = adapter;
        this.clusterType = config.getAdapter();
    }

    public CreateSubmissionResponse create(CreateSubmissionRequest request) {
        SparkSubmission submission = new SparkSubmission();
        submission.setClusterType(clusterType);
        submission.setAppResource(request.appResource());
        submission.setMainClass(request.mainClass());
        try {
            submission.setAppArgs(objectMapper.writeValueAsString(request.appArgs()));
        } catch (Exception e) {
            submission.setAppArgs("[]");
        }
        try {
            submission.setSparkProperties(objectMapper.writeValueAsString(request.sparkProperties()));
        } catch (Exception e) {
            submission.setSparkProperties("{}");
        }
        submission.setStatus(SparkSubmission.Status.QUEUED);

        repository.save(submission);
        log.info("Created submission task id={}", submission.getId());

        return new CreateSubmissionResponse(
                "CreateSubmissionResponse",
                ExternalStatus.WAITING.name(),
                null,
                String.valueOf(submission.getId()),
                true
        );
    }

    public SparkSubmission getSubmission(Long id) {
        return repository.findById(id).orElse(null);
    }

    public SubmissionStatusResponse getStatus(Long id) {
        SparkSubmission submission = repository.findById(id).orElse(null);
        if (submission == null) {
            return new SubmissionStatusResponse("StatusResponse", "NOT_FOUND", null, String.valueOf(id), false, "UNKNOWN", null, null);
        }

        String internalStatus = submission.getStatus().name();

        if (internalStatus.equals("QUEUED") || internalStatus.equals("CLAIMED")) {
            return new SubmissionStatusResponse(
                    "StatusResponse",
                    "Task is " + internalStatus.toLowerCase(),
                    null,
                    String.valueOf(id),
                    true,
                    ExternalStatus.WAITING.name(),
                    null, null
            );
        }

        String realSubmissionId = submission.getSubmissionId();
        if (realSubmissionId == null) {
            String externalStatus = ExternalStatus.fromInternal(internalStatus).name();
            return new SubmissionStatusResponse(
                    "StatusResponse",
                    externalStatus,
                    null,
                    String.valueOf(id),
                    true,
                    externalStatus,
                    null, null
            );
        }

        return adapter.getSubmissionStatus(realSubmissionId);
    }

    public CreateSubmissionResponse kill(Long id) {
        SparkSubmission submission = repository.findById(id).orElse(null);
        if (submission == null) {
            return new CreateSubmissionResponse
                    ("KillResponse",
                            "NOT_FOUND",
                            null,
                            String.valueOf(id), false);
        }

        String realSubmissionId = submission.getSubmissionId();

        if (realSubmissionId == null) {
            repository.delete(submission);
            log.info("Deleted queued task id={}", id);
            return new CreateSubmissionResponse("KillResponse", ExternalStatus.KILLED.name(), null, String.valueOf(id), true);
        }

        CreateSubmissionResponse result = adapter.killSubmission(realSubmissionId);
        if (Boolean.TRUE.equals(result.success())) {
            repository.delete(submission);
            log.info("Killed and deleted submission id={}, submissionId={}", id, realSubmissionId);
        }
        return result;
    }

    public CreateSubmissionResponse killAll() {
        int deletedQueued = 0;

        for (SparkSubmission sub : repository.findByStatus(SparkSubmission.Status.QUEUED)) {
            repository.delete(sub);
            deletedQueued++;
        }
        for (SparkSubmission sub : repository.findByStatus(SparkSubmission.Status.CLAIMED)) {
            repository.delete(sub);
            deletedQueued++;
        }

        log.warn("Deleted {} queued/claimed tasks", deletedQueued);
        return new CreateSubmissionResponse(
                "KillAllResponse",
                ExternalStatus.KILLED.name() + " " + deletedQueued + " queued/claimed tasks",
                null, null, true
        );
    }

    public CreateSubmissionResponse clear() {
        int cleared = 0;

        for (SparkSubmission sub : repository.findByStatus(SparkSubmission.Status.COMPLETED)) {
            adapter.clearCompleted();
            repository.delete(sub);
            cleared++;
        }

        return new CreateSubmissionResponse("ClearResponse", "Cleared " + cleared + " submissions", null, null, true);
    }
}
