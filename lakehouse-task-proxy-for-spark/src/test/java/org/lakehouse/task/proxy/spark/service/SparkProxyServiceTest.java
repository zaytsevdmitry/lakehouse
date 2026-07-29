package org.lakehouse.task.proxy.spark.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lakehouse.task.proxy.spark.adapter.SparkAdapter;
import org.lakehouse.task.proxy.spark.config.ProxyConfig;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.SubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.ExternalStatus;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.entity.SparkSubmission;
import org.lakehouse.task.proxy.spark.repository.SparkSubmissionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SparkProxyServiceTest {

    @Mock
    private SparkSubmissionRepository repository;

    @Mock
    private SparkAdapter adapter;

    @Mock
    private TransactionTemplate transactionTemplate;

    private SparkProxyService service;

    @BeforeEach
    void setUp() {
        ProxyConfig config = new ProxyConfig();
        config.setAdapter("standalone");
        service = new SparkProxyService(repository, adapter, transactionTemplate, config);
        lenient().when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            TransactionCallback<Object> callback = inv.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    // --- create ---

    @Test
    void create_savesSubmissionAndReturnsWaiting() {
        when(repository.save(any(SparkSubmission.class))).thenAnswer(inv -> {
            SparkSubmission s = inv.getArgument(0);
            s.setId(42L);
            return s;
        });

        CreateSubmissionRequest request = new CreateSubmissionRequest(
                null, "app.jar", null, "com.Main", Map.of(), null
        );

        SubmissionResponse response = service.create(request);

        assertEquals("42", response.submissionId());
        assertEquals(ExternalStatus.WAITING.name(), response.message());
        assertTrue(response.success());
        verify(repository).save(any(SparkSubmission.class));
    }

    // --- getStatus ---

    @Test
    void getStatus_notFound_returnsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        SubmissionStatusResponse response = service.getStatus(99L);
        assertEquals("NOT_FOUND", response.message());
        assertFalse(response.success());
        assertEquals("UNKNOWN", response.driverState());
    }

    @Test
    void getStatus_queued_returnsWaiting() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.WAITING);
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        SubmissionStatusResponse response = service.getStatus(1L);
        assertEquals(ExternalStatus.WAITING.name(), response.driverState());
        assertTrue(response.success());
    }

    @Test
    void getStatus_submitted_returnsSubmittedFromDb() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.SUBMITTED);
        sub.setSubmissionId("driver-abc-123");
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        SubmissionStatusResponse response = service.getStatus(1L);
        assertEquals(ExternalStatus.SUBMITTED.name(), response.driverState());
        assertEquals("driver-abc-123", response.submissionId());
        assertTrue(response.success());
        verifyNoInteractions(adapter);
    }

    @Test
    void getStatus_running_returnsRunning() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.RUNNING);
        sub.setSubmissionId("driver-abc-123");
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        SubmissionStatusResponse response = service.getStatus(1L);
        assertEquals(ExternalStatus.RUNNING.name(), response.driverState());
        assertTrue(response.success());
        verifyNoInteractions(adapter);
    }

    @Test
    void getStatus_finished_returnsFinished() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.FINISHED);
        sub.setSubmissionId("driver-abc-123");
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        SubmissionStatusResponse response = service.getStatus(1L);
        assertEquals(ExternalStatus.FINISHED.name(), response.driverState());
        assertTrue(response.success());
    }

    @Test
    void getStatus_killed_returnsKilled() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.KILLED);
        sub.setSubmissionId("driver-abc-123");
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        SubmissionStatusResponse response = service.getStatus(1L);
        assertEquals(ExternalStatus.KILLED.name(), response.driverState());
        assertTrue(response.success());
    }

    @Test
    void getStatus_unknown_returnsUnknown() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.UNKNOWN);
        sub.setSubmissionId("driver-abc-123");
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        SubmissionStatusResponse response = service.getStatus(1L);
        assertEquals(ExternalStatus.UNKNOWN.name(), response.driverState());
        assertTrue(response.success());
    }

    // --- kill ---

    @Test
    void kill_notFound_returnsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        SubmissionResponse response = service.kill(99L);
        assertEquals("NOT_FOUND", response.message());
        assertFalse(response.success());
    }

    @Test
    void kill_queuedTask_deletesFromDb() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.WAITING);
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        SubmissionResponse response = service.kill(1L);
        assertEquals(ExternalStatus.KILLED.name(), response.message());
        assertTrue(response.success());
        verify(repository).delete(sub);
    }

    @Test
    void kill_submittedTask_callsAdapter() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.SUBMITTED);
        sub.setSubmissionId("driver-abc-123");
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        when(adapter.killSubmission(eq("driver-abc-123")))
                .thenReturn(new SubmissionResponse("KillResponse", ExternalStatus.KILLED.name(), null, "driver-abc-123", true));

        SubmissionResponse response = service.kill(1L);
        assertEquals(ExternalStatus.KILLED.name(), response.message());
        verify(adapter).killSubmission(eq("driver-abc-123"));
        verify(repository).delete(sub);
    }

    @Test
    void kill_adapterFails_doesNotDelete() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.SUBMITTED);
        sub.setSubmissionId("driver-abc-123");
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        when(adapter.killSubmission(eq("driver-abc-123")))
                .thenReturn(new SubmissionResponse("KillResponse", ExternalStatus.FAILED.name(), null, "driver-abc-123", false));

        service.kill(1L);
        verify(repository, never()).delete(any());
    }

    // --- killAll ---

    @Test
    void killAll_deletesQueued() {
        SparkSubmission q1 = new SparkSubmission();
        q1.setId(1L);
        SparkSubmission q2 = new SparkSubmission();
        q2.setId(2L);

        when(repository.findByStatus(SparkSubmission.Status.WAITING)).thenReturn(List.of(q1, q2));

        SubmissionResponse response = service.killAll();
        assertTrue(response.success());
        assertTrue(response.message().contains("2"));
        verify(repository).delete(q1);
        verify(repository).delete(q2);
    }

    @Test
    void killAll_noTasks() {
        when(repository.findByStatus(SparkSubmission.Status.WAITING)).thenReturn(List.of());

        SubmissionResponse response = service.killAll();
        assertTrue(response.success());
        assertTrue(response.message().contains("0"));
    }

    // --- clear ---

    @Test
    void clear_deletesCompletedSubmissions() {
        when(repository.claimAllTasks(10000))
                .thenReturn(List.<Object[]>of(
                        new Object[]{1L, "driver-abc-123", "FINISHED"}));
        when(adapter.clearCompleted(eq("driver-abc-123")))
                .thenReturn(new SubmissionResponse("ClearResponse", ExternalStatus.FINISHED.name(), null, "driver-abc-123", true));

        SubmissionResponse response = service.clear();
        assertTrue(response.success());
        assertTrue(response.message().contains("1"));
        assertTrue(response.message().contains("0"));
        verify(adapter).clearCompleted(eq("driver-abc-123"));
        verify(repository).deleteById(1L);
        verify(adapter).postClear();
    }

    @Test
    void clear_killsRunningSubmissions() {
        when(repository.claimAllTasks(10000))
                .thenReturn(List.<Object[]>of(
                        new Object[]{2L, "driver-xyz-789", "RUNNING"}));
        when(adapter.killSubmission(eq("driver-xyz-789")))
                .thenReturn(new SubmissionResponse("KillResponse", ExternalStatus.KILLED.name(), null, "driver-xyz-789", true));

        SubmissionResponse response = service.clear();
        assertTrue(response.success());
        assertTrue(response.message().contains("1"));
        assertTrue(response.message().contains("1"));
        verify(adapter).killSubmission(eq("driver-xyz-789"));
        verify(repository).deleteById(2L);
        verify(adapter).postClear();
    }

    @Test
    void clear_noRows() {
        when(repository.claimAllTasks(10000)).thenReturn(List.<Object[]>of());

        SubmissionResponse response = service.clear();
        assertTrue(response.success());
        assertTrue(response.message().contains("0"));
        verify(adapter).postClear();
    }

    @Test
    void clear_submissionWithNullSubmissionId_skipsAdapter() {
        when(repository.claimAllTasks(10000))
                .thenReturn(List.<Object[]>of(
                        new Object[]{3L, null, "WAITING"}));

        SubmissionResponse response = service.clear();
        assertTrue(response.success());
        assertTrue(response.message().contains("1"));
        verify(adapter, never()).clearCompleted(any());
        verify(adapter, never()).killSubmission(any());
        verify(repository).deleteById(3L);
        verify(adapter).postClear();
    }

    @Test
    void clear_adapterFails_stillDeletes() {
        when(repository.claimAllTasks(10000))
                .thenReturn(List.<Object[]>of(
                        new Object[]{4L, "driver-fail-456", "FINISHED"}));
        when(adapter.clearCompleted(eq("driver-fail-456")))
                .thenReturn(new SubmissionResponse("ClearResponse", ExternalStatus.FAILED.name(), null, "driver-fail-456", false));

        SubmissionResponse response = service.clear();
        assertTrue(response.success());
        assertTrue(response.message().contains("1"));
        verify(adapter).clearCompleted(eq("driver-fail-456"));
        verify(repository).deleteById(4L);
        verify(adapter).postClear();
    }
}
