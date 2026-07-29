package org.lakehouse.task.proxy.spark.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.lakehouse.task.proxy.spark.adapter.SparkAdapter;
import org.lakehouse.task.proxy.spark.config.ProxyConfig;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.ExternalStatus;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.entity.SparkSubmission;
import org.lakehouse.task.proxy.spark.repository.SparkSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class SparkProxyService {

    private static final Logger log = LoggerFactory.getLogger(SparkProxyService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SparkSubmissionRepository repository;
    private final SparkAdapter adapter;
    private final TransactionTemplate transactionTemplate;
    private final String clusterType;

    public SparkProxyService(SparkSubmissionRepository repository, SparkAdapter adapter,
                              TransactionTemplate transactionTemplate, ProxyConfig config) {
        this.repository = repository;
        this.adapter = adapter;
        this.transactionTemplate = transactionTemplate;
        this.clusterType = config.getAdapter();
    }

    public SubmissionResponse create(CreateSubmissionRequest request) {
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
        submission.setStatus(SparkSubmission.Status.WAITING);

        repository.save(submission);
        log.info("Created submission task id={}", submission.getId());

        return new SubmissionResponse(
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

        String status = submission.getStatus().name();
        String externalStatus = ExternalStatus.fromInternal(status).name();

        return new SubmissionStatusResponse(
                "StatusResponse",
                submission.getMessage() != null ? submission.getMessage() : externalStatus,
                null,
                submission.getSubmissionId(),
                true,
                externalStatus,
                null, null
        );
    }

    public SubmissionResponse kill(Long id) {
        SparkSubmission submission = repository.findById(id).orElse(null);
        if (submission == null) {
            return new SubmissionResponse
                    ("KillResponse",
                            "NOT_FOUND",
                            null,
                            String.valueOf(id), false);
        }

        String realSubmissionId = submission.getSubmissionId();

        if (realSubmissionId == null) {
            repository.delete(submission);
            log.info("Deleted queued task id={}", id);
            return new SubmissionResponse("KillResponse", ExternalStatus.KILLED.name(), null, String.valueOf(id), true);
        }

        SubmissionResponse result = adapter.killSubmission(realSubmissionId);
        if (Boolean.TRUE.equals(result.success())) {
            repository.delete(submission);
            log.info("Killed and deleted submission id={}, submissionId={}", id, realSubmissionId);
        }
        return result;
    }

    public SubmissionResponse killAll() {
        int deletedQueued = 0;

        for (SparkSubmission sub : repository.findByStatus(SparkSubmission.Status.WAITING)) {
            repository.delete(sub);
            deletedQueued++;
        }

        log.warn("Deleted {} queued tasks", deletedQueued);
        return new SubmissionResponse(
                "KillAllResponse",
                ExternalStatus.KILLED.name() + " " + deletedQueued + " queued tasks",
                null, null, true
        );
    }

    public SubmissionResponse clear() {
        record ClearResult(List<Long> toDelete, int killed) {}

        ClearResult result = transactionTemplate.execute(status -> {
            List<Long> ids = new ArrayList<>();
            int killCount = 0;
            List<Object[]> rows = repository.claimAllTasks(10000);
            for (Object[] row : rows) {
                Long id = ((Number) row[0]).longValue();
                String submissionId = (String) row[1];
                String statusStr = (String) row[2];

                if (submissionId == null) {
                    ids.add(id);
                    continue;
                }

                boolean isTerminal = statusStr != null && SparkSubmission.isFinalStatus(
                        SparkSubmission.Status.valueOf(statusStr));
                if (isTerminal) {
                    try {
                        SubmissionResponse r = adapter.clearCompleted(submissionId);
                        if (!Boolean.TRUE.equals(r.success())) {
                            log.warn("clearCompleted returned failure for submission {}: {}", submissionId, r.message());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to clearCompleted submission {}: {}", submissionId, e.getMessage());
                    }
                } else {
                    try {
                        SubmissionResponse r = adapter.killSubmission(submissionId);
                        if (Boolean.TRUE.equals(r.success())) {
                            killCount++;
                        }
                    } catch (Exception e) {
                        log.warn("Failed to kill submission {}: {}", submissionId, e.getMessage());
                    }
                }
                ids.add(id);
            }
            return new ClearResult(ids, killCount);
        });

        for (Long id : result.toDelete()) {
            repository.deleteById(id);
        }

        adapter.postClear();

        log.info("Clear completed: deleted {} records, killed={}", result.toDelete().size(), result.killed());
        return new SubmissionResponse("ClearResponse",
                "Cleared " + result.toDelete().size() + " submissions (killed " + result.killed() + ")", null, null, true);
    }
}
