package org.lakehouse.task.proxy.spark.scheduler;

import jakarta.annotation.PreDestroy;
import org.lakehouse.task.proxy.spark.adapter.SparkAdapter;
import org.lakehouse.task.proxy.spark.config.ProxyConfig;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.repository.SparkSubmissionRepository;
import org.lakehouse.task.proxy.spark.service.SparkMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Component
public class CleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(CleanupScheduler.class);

    private final SparkSubmissionRepository repository;
    private final SparkAdapter adapter;
    private final TransactionTemplate transactionTemplate;
    private final ScheduledExecutorService schedulerExecutor;
    private final long pollIntervalMs;
    private final int poolSize;
    private final int batchSize;
    private final long retentionSeconds;
    private final List<ScheduledFuture<?>> futures = new CopyOnWriteArrayList<>();

    public CleanupScheduler(SparkSubmissionRepository repository,
                            SparkAdapter adapter,
                            TransactionTemplate transactionTemplate,
                            SparkMetrics sparkMetrics,
                            ProxyConfig config) {
        this.repository = repository;
        this.adapter = adapter;
        this.transactionTemplate = transactionTemplate;
        this.pollIntervalMs = config.getCleanup().getPollIntervalMs();
        this.batchSize = config.getCleanup().getBatchSize();
        this.poolSize = config.getCleanup().getPoolSize();
        this.retentionSeconds = config.getCleanup().getRetentionSeconds();
        this.schedulerExecutor = Executors.newScheduledThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "cleanup-scheduler");
            t.setDaemon(true);
            return t;
        });
        startPolling();
        log.info("CleanupScheduler initialized. poolSize={}, pollIntervalMs={}, batchSize={}, retentionSeconds={}",
                poolSize, pollIntervalMs, batchSize, retentionSeconds);
    }

    private void startPolling() {
        for (int i = 0; i < poolSize; i++) {
            futures.add(schedulerExecutor.scheduleWithFixedDelay(
                    this::processBatch, 0, pollIntervalMs, TimeUnit.MILLISECONDS));
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down CleanupScheduler");
        futures.forEach(f -> f.cancel(false));
        schedulerExecutor.shutdownNow();
    }

    private void processBatch() {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                List<Object[]> rows = repository.claimForCleanup(batchSize, retentionSeconds);
                if (rows.isEmpty()) {
                    return;
                }

                List<Long> toDelete = new ArrayList<>();

                for (Object[] row : rows) {
                    Long id = ((Number) row[0]).longValue();
                    String submissionId = (String) row[1];

                    SubmissionResponse result = adapter.clearCompleted(submissionId);
                    if (Boolean.TRUE.equals(result.success())) {
                        toDelete.add(id);
                    } else {
                        log.warn("Failed to clear submission {} in cluster, will retry", submissionId);
                    }
                }

                if (!toDelete.isEmpty()) {
                    repository.deleteAllIds(toDelete);
                    log.info("Cleanup completed: deleted {} terminal records", toDelete.size());
                }
            });
        } catch (Exception e) {
            log.error("CleanupScheduler processBatch error", e);
        }
    }
}
