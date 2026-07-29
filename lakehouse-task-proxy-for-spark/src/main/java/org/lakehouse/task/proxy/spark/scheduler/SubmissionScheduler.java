package org.lakehouse.task.proxy.spark.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PreDestroy;
import org.lakehouse.task.proxy.spark.adapter.SparkAdapter;
import org.lakehouse.task.proxy.spark.config.ProxyConfig;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.repository.SparkSubmissionRepository;
import org.lakehouse.task.proxy.spark.service.SparkMetrics;
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
public class SubmissionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubmissionScheduler.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final SparkSubmissionRepository repository;
    private final SparkAdapter adapter;
    private final TransactionTemplate transactionTemplate;
    private final SparkMetrics sparkMetrics;
    private final String instanceId;
    private final ScheduledExecutorService schedulerExecutor;
    private final long pollIntervalMs;
    private final String clusterType;
    private final List<ScheduledFuture<?>> futures = new java.util.concurrent.CopyOnWriteArrayList<>();

    public SubmissionScheduler(SparkSubmissionRepository repository,
                               SparkAdapter adapter,
                               TransactionTemplate transactionTemplate,
                               SparkMetrics sparkMetrics,
                               ProxyConfig config) {
        this.repository = repository;
        this.adapter = adapter;
        this.transactionTemplate = transactionTemplate;
        this.sparkMetrics = sparkMetrics;
        this.pollIntervalMs = config.getScheduler().getPollIntervalMs();
        this.clusterType = config.getAdapter();
        int poolSize = config.getScheduler().getPoolSize();
        this.instanceId = resolveInstanceId();
        this.schedulerExecutor = Executors.newScheduledThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "scheduler-poll-" + instanceId);
            t.setDaemon(true);
            return t;
        });
        startPolling(poolSize);
        log.info("Scheduler initialized. instanceId={}, poolSize={}, pollIntervalMs={}, clusterType={}",
                this.instanceId, poolSize, this.pollIntervalMs, this.clusterType);
    }

    private void startPolling(int poolSize) {
        for (int i = 0; i < poolSize; i++) {
            futures.add(schedulerExecutor.scheduleWithFixedDelay(
                    this::poll, 0, pollIntervalMs, TimeUnit.MILLISECONDS));
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down scheduler for instance={}", instanceId);
        futures.forEach(f -> f.cancel(false));
        schedulerExecutor.shutdownNow();
    }

    public void poll() {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                Object r = repository.claimNextTask();
                if (r == null) {
                    return;
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
                    return;
                }

                if (flatColumns.length == 0 || flatColumns[0] == null) {
                    return;
                }

                Long taskId = ((Number) flatColumns[0]).longValue();
                String appResource = (String) flatColumns[3];
                String mainClass = (String) flatColumns[4];
                String appArgsJson = (String) flatColumns[5];
                String sparkPropertiesJson = (String) flatColumns[6];

                log.info("Claimed task id={} on instance={}", taskId, instanceId);

                Map<String, String> sparkProperties = deserializeProperties(sparkPropertiesJson);
                List<String> appArgs = deserializeAppArgs(appArgsJson);
                CreateSubmissionRequest request = new CreateSubmissionRequest(
                        null, appArgs, appResource, null, mainClass, sparkProperties, null
                );

                sparkMetrics.recordRequest(clusterType);
                Timer.Sample sample = sparkMetrics.startTimer();

                try {
                    String submissionId = adapter.createSubmission(request);
                    sparkMetrics.recordSuccess(clusterType);
                    sparkMetrics.recordDuration(sample, clusterType);
                    repository.completeTask(taskId, submissionId, "SUBMITTED", "spark-submit launched successfully");
                    log.info("Task id={} submitted. submissionId={}", taskId, submissionId);
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                    if (msg.contains("Timeout")) {
                        sparkMetrics.recordTimeout(clusterType);
                    } else {
                        sparkMetrics.recordFailed(clusterType);
                    }
                    sparkMetrics.recordDuration(sample, clusterType);
                    log.error("Task id={} submission failed: {}", taskId, msg, e);
                    markFailed(taskId, msg);
                }
            });
        } catch (Exception e) {
            log.error("Scheduler poll error", e);
        }
    }

    private void markFailed(Long id, String message) {
        if (id == null) {
            return;
        }
        try {
            String safeMessage = message.length() > 255 ? message.substring(0, 252) + "..." : message;
                    repository.completeTask(id, null, "ERROR", safeMessage);
        } catch (Exception ex) {
            log.error("Failed to mark task id={} as FAILED", id, ex);
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
