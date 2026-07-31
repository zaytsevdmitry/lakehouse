package org.lakehouse.task.proxy.spark.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lakehouse.task.proxy.spark.adapter.SparkAdapter;
import org.lakehouse.task.proxy.spark.config.ProxyConfig;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.repository.SparkSubmissionRepository;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CleanupSchedulerTest {

    @Mock
    private SparkSubmissionRepository repository;

    @Mock
    private SparkAdapter adapter;

    private TransactionTemplate transactionTemplate;
    private CleanupScheduler scheduler;
    private Method processBatchMethod;

    @BeforeEach
    void setUp() throws Exception {
        ProxyConfig config = new ProxyConfig();
        config.getCleanup().setPollIntervalMs(60000);
        config.getCleanup().setPoolSize(1);
        config.getCleanup().setBatchSize(50);
        config.getCleanup().setRetentionSeconds(3600);

        transactionTemplate = mock(TransactionTemplate.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));

        doAnswer(inv -> {
            TransactionCallback<Void> callback = inv.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        }).when(transactionTemplate).execute(any());

        try (MockedStatic<Executors> executors = mockStatic(Executors.class)) {
            ScheduledExecutorService unused = mock(ScheduledExecutorService.class);
            when(unused.scheduleWithFixedDelay(any(), anyLong(), anyLong(), any())).thenReturn(mock(ScheduledFuture.class));
            executors.when(() -> Executors.newScheduledThreadPool(anyInt(), any())).thenReturn(unused);

            scheduler = new CleanupScheduler(repository, adapter, transactionTemplate, null, config);
        }

        processBatchMethod = CleanupScheduler.class.getDeclaredMethod("processBatch");
        processBatchMethod.setAccessible(true);
    }

    @Nested
    class ProcessBatch {

        @Test
        void noRows_doesNothing() throws Exception {
            when(repository.claimForCleanup(50, 3600)).thenReturn(List.of());

            processBatchMethod.invoke(scheduler);

            verify(repository).claimForCleanup(50, 3600);
            verifyNoInteractions(adapter);
            verify(repository, never()).deleteAllIds(any());
        }

        @Test
        void deletesAfterClearSucceeds() throws Exception {
            when(repository.claimForCleanup(50, 3600))
                    .thenReturn(List.<Object[]>of(
                            new Object[]{1L, "driver-finished-001"},
                            new Object[]{2L, "driver-finished-002"}));
            when(adapter.clearCompleted("driver-finished-001"))
                    .thenReturn(new SubmissionResponse("ClearResponse", null, null, "driver-finished-001", true));
            when(adapter.clearCompleted("driver-finished-002"))
                    .thenReturn(new SubmissionResponse("ClearResponse", null, null, "driver-finished-002", true));

            processBatchMethod.invoke(scheduler);

            verify(adapter).clearCompleted("driver-finished-001");
            verify(adapter).clearCompleted("driver-finished-002");
            verify(repository).deleteAllIds(List.of(1L, 2L));
        }

        @Test
        void skipsRowWhenClearFails() throws Exception {
            when(repository.claimForCleanup(50, 3600))
                    .thenReturn(List.<Object[]>of(
                            new Object[]{1L, "driver-ok"},
                            new Object[]{2L, "driver-fail"}));
            when(adapter.clearCompleted("driver-ok"))
                    .thenReturn(new SubmissionResponse("ClearResponse", null, null, "driver-ok", true));
            when(adapter.clearCompleted("driver-fail"))
                    .thenReturn(new SubmissionResponse("ClearResponse", "FAILED", null, "driver-fail", false));

            processBatchMethod.invoke(scheduler);

            verify(adapter).clearCompleted("driver-ok");
            verify(adapter).clearCompleted("driver-fail");
            verify(repository).deleteAllIds(List.of(1L));
        }

        @Test
        void skipsRowWhenClearThrows() throws Exception {
            when(repository.claimForCleanup(50, 3600))
                    .thenReturn(List.<Object[]>of(
                            new Object[]{1L, "driver-ok"},
                            new Object[]{2L, "driver-ex"}));
            when(adapter.clearCompleted("driver-ok"))
                    .thenReturn(new SubmissionResponse("ClearResponse", null, null, "driver-ok", true));
            doThrow(new RuntimeException("K8s API error"))
                    .when(adapter).clearCompleted("driver-ex");

            processBatchMethod.invoke(scheduler);

            verify(adapter).clearCompleted("driver-ok");
            verify(adapter).clearCompleted("driver-ex");
            verify(repository).deleteAllIds(List.of(1L));
        }

        @Test
        void exceptionInTransactionCatchOuter() throws Exception {
            reset(transactionTemplate);
            doThrow(new RuntimeException("DB error"))
                    .when(transactionTemplate).execute(any());

            processBatchMethod.invoke(scheduler);

            verify(repository, never()).claimForCleanup(anyInt(), anyLong());
            verifyNoInteractions(adapter);
        }
    }
}
