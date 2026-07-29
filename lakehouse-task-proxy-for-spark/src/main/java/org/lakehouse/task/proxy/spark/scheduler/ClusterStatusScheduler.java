package org.lakehouse.task.proxy.spark.scheduler;

import jakarta.annotation.PreDestroy;
import org.lakehouse.task.proxy.spark.adapter.SparkAdapter;
import org.lakehouse.task.proxy.spark.config.ProxyConfig;
import org.lakehouse.task.proxy.spark.dto.ExternalStatus;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.repository.SparkSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class ClusterStatusScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClusterStatusScheduler.class);

    private final SparkSubmissionRepository repository;
    private final SparkAdapter adapter;
    private final TransactionTemplate transactionTemplate;
    private final ScheduledExecutorService schedulerExecutor;
    private final long pollIntervalMs;
    private final int poolSize;
    private final int batchSize;
    private final List<ScheduledFuture<?>> futures = new CopyOnWriteArrayList<>();

    public ClusterStatusScheduler(SparkSubmissionRepository repository,
                                  SparkAdapter adapter,
                                  TransactionTemplate transactionTemplate,
                                  ProxyConfig config) {
        this.repository = repository;
        this.adapter = adapter;
        this.transactionTemplate = transactionTemplate;
        this.pollIntervalMs = config.getInspection().getPollIntervalMs();
        this.batchSize = config.getInspection().getBatchSize();
        this.poolSize = config.getInspection().getPoolSize();
        this.schedulerExecutor = Executors.newScheduledThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "cluster-status-inspector");
            t.setDaemon(true);
            return t;
        });
        startPolling();
        log.info("ClusterStatusScheduler initialized. poolSize={}, pollIntervalMs={}, batchSize={}",
                poolSize, pollIntervalMs, batchSize);
    }

    private void startPolling() {
        for (int i = 0; i < poolSize; i++) {
            futures.add(schedulerExecutor.scheduleWithFixedDelay(
                    this::processBatch, 0, pollIntervalMs, TimeUnit.MILLISECONDS));
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ClusterStatusScheduler");
        futures.forEach(f -> f.cancel(false));
        schedulerExecutor.shutdownNow();
    }

    private void processBatch() {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                List<Object[]> rows = repository.claimIncompleteTasks(batchSize);
                if (rows.isEmpty()) {
                    return;
                }

                log.debug("Claimed {} incomplete tasks for inspection", rows.size());

                for (Object[] row : rows) {
                    Long id = ((Number) row[0]).longValue();
                    String submissionId = (String) row[1];
                    String clusterType = (String) row[2];

                    if (submissionId == null) {
                        continue;
                    }

                    try {
                        SubmissionStatusResponse response = adapter.getSubmissionStatus(submissionId);
                        String driverState = response.driverState();
                        ExternalStatus externalStatus = ExternalStatus.fromInternal(driverState);
                        String newStatus = externalStatus.name();
                        String message = response.message() != null ? response.message() : driverState;

                        repository.updateStatus(id, newStatus, message);
                        log.debug("Task id={} status updated to {} (submissionId={})", id, newStatus, submissionId);
                    } catch (Exception e) {
                        String msg = e.getMessage() != null ? e.getMessage() : "Unknown error";
                        repository.updateStatus(id, ExternalStatus.UNKNOWN.name(), msg);
                        log.warn("Task id={} status check failed, set UNKNOWN: {}", id, msg);
                    }
                }
            });
        } catch (Exception e) {
            log.error("ClusterStatusScheduler processBatch error", e);
        }
    }
}
