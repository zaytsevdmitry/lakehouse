package org.lakehouse.task.proxy.spark.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import org.lakehouse.task.proxy.spark.adapter.SparkAdapter;
import org.lakehouse.task.proxy.spark.config.ProxyConfig;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.repository.SparkSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class SparkSubmissionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SparkSubmissionScheduler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SparkSubmissionRepository repository;
    private final SparkAdapter adapter;
    private final TransactionTemplate transactionTemplate;
    private final String instanceId;
    private final ScheduledExecutorService executorService;
    private final long pollIntervalMs;
    private final List<ScheduledFuture<?>> futures = new java.util.concurrent.CopyOnWriteArrayList<>();

    public SparkSubmissionScheduler(SparkSubmissionRepository repository,
                                    SparkAdapter adapter,
                                    TransactionTemplate transactionTemplate,
                                    ProxyConfig config) {
        this.repository = repository;
        this.adapter = adapter;
        this.transactionTemplate = transactionTemplate;
        this.pollIntervalMs = config.getScheduler().getPollIntervalMs();
        int poolSize = config.getScheduler().getPoolSize();
        this.instanceId = resolveInstanceId();
        this.executorService = Executors.newScheduledThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "scheduler-poll-" + instanceId);
            t.setDaemon(true);
            return t;
        });
        startPolling(poolSize);
        log.info("Scheduler initialized. instanceId={}, poolSize={}, pollIntervalMs={}", this.instanceId, poolSize, pollIntervalMs);
    }

    private void startPolling(int poolSize) {
        for (int i = 0; i < poolSize; i++) {
            futures.add(executorService.scheduleWithFixedDelay(
                    this::poll, 0, pollIntervalMs, TimeUnit.MILLISECONDS));
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down scheduler executor for instance={}", instanceId);
        futures.forEach(f -> f.cancel(false));
        executorService.shutdownNow();
    }

    public void poll() {
        Long id = null;
        try {
            Object[] row = transactionTemplate.execute(status -> {
                Object r = repository.claimNextTask();
                if (r == null) {
                    return null;
                }

                Object[] flatColumns;
                if (r instanceof Object[][] && ((Object[][]) r).length > 0) {
                    flatColumns = ((Object[][]) r)[0];
                } else if (r instanceof Object[] && ((Object[]) r).length > 0 && ((Object[]) r)[0] instanceof Object[]) {
                    flatColumns = (Object[]) ((Object[]) r)[0];
                } else if (r instanceof Object[]) {
                    flatColumns = (Object[]) r;
                } else {
                    log.error("Unexpected claimNextTask return structure type: {}", r.getClass().getName());
                    return null;
                }

                if (flatColumns.length == 0 || flatColumns[0] == null) {
                    return null;
                }

                Long taskId = ((Number) flatColumns[0]).longValue();
                repository.markClaimed(taskId, instanceId);

                return flatColumns;
            });

            if (row == null) {
                return;
            }

            id = ((Number) row[0]).longValue();
            String appResource = (String) row[3];
            String mainClass = (String) row[4];
            String appArgsJson = (String) row[5];
            String sparkPropertiesJson = (String) row[6];

            log.info("Claimed task id={} on instance={}", id, instanceId);

            Map<String, String> sparkProperties = deserializeProperties(sparkPropertiesJson);
            List<String> appArgs = deserializeAppArgs(appArgsJson);
            CreateSubmissionRequest request = new CreateSubmissionRequest(
                    null, appArgs, appResource, null, mainClass, sparkProperties, null
            );

            String submissionId = adapter.createSubmission(request);
            repository.completeTask(id, submissionId, "SUBMITTED", "spark-submit launched successfully");
            log.info("Task id={} submitted. submissionId={}", id, submissionId);

        } catch (Exception e) {
            log.error("Scheduler poll error for task id={}", id, e);
            if (id != null) {
                try {
                    String fullError = e.getMessage() != null ? e.getMessage() : "Unknown scheduler error";
                    String safeMessage = fullError.length() > 255 ? fullError.substring(0, 252) + "..." : fullError;

                    repository.completeTask(id, null, "FAILED", safeMessage);
                } catch (Exception ex) {
                    log.error("Failed to mark task id={} as FAILED", id, ex);
                }
            }
        }
    }

    private Map<String, String> deserializeProperties(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize sparkProperties: {}", json, e);
            return Map.of();
        }
    }

    private List<String> deserializeAppArgs(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to deserialize appArgs: {}", json, e);
            return List.of();
        }
    }

    private String resolveInstanceId() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            return host + "-" + ProcessHandle.current().pid();
        } catch (Exception e) {
            return "instance-" + ProcessHandle.current().pid();
        }
    }
}
