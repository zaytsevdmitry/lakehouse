package org.lakehouse.task.proxy.spark.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lakehouse.task.proxy.spark.adapter.SparkAdapter;
import org.lakehouse.task.proxy.spark.config.ProxyConfig;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionRequest;
import org.lakehouse.task.proxy.spark.dto.CreateSubmissionResponse;
import org.lakehouse.task.proxy.spark.dto.ExternalStatus;
import org.lakehouse.task.proxy.spark.dto.SubmissionStatusResponse;
import org.lakehouse.task.proxy.spark.entity.SparkSubmission;
import org.lakehouse.task.proxy.spark.repository.SparkSubmissionRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    private SparkProxyService service;

    @BeforeEach
    void setUp() {
        ProxyConfig config = new ProxyConfig();
        config.setAdapter("standalone");
        service = new SparkProxyService(repository, adapter, config);
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

        CreateSubmissionResponse response = service.create(request);

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
        sub.setStatus(SparkSubmission.Status.QUEUED);
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        SubmissionStatusResponse response = service.getStatus(1L);
        assertEquals(ExternalStatus.WAITING.name(), response.driverState());
        assertTrue(response.success());
    }

    @Test
    void getStatus_claimed_returnsWaiting() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.CLAIMED);
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        SubmissionStatusResponse response = service.getStatus(1L);
        assertEquals(ExternalStatus.WAITING.name(), response.driverState());
        assertTrue(response.success());
    }

    @Test
    void getStatus_submitted_returnsWaiting() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.SUBMITTED);
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        SubmissionStatusResponse response = service.getStatus(1L);
        assertEquals(ExternalStatus.WAITING.name(), response.driverState());
        assertTrue(response.success());
    }

    @Test
    void getStatus_completedNoSubmissionId_returnsFinished() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.COMPLETED);
        sub.setSubmissionId(null);
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        SubmissionStatusResponse response = service.getStatus(1L);
        assertEquals(ExternalStatus.FINISHED.name(), response.driverState());
    }

    @Test
    void getStatus_failedNoSubmissionId_returnsFailed() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.FAILED);
        sub.setSubmissionId(null);
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        SubmissionStatusResponse response = service.getStatus(1L);
        assertEquals(ExternalStatus.FAILED.name(), response.driverState());
    }

    @Test
    void getStatus_withRealSubmissionId_queriesAdapter() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.COMPLETED);
        sub.setSubmissionId("driver-abc-123");
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        when(adapter.getSubmissionStatus(eq("driver-abc-123")))
                .thenReturn(new SubmissionStatusResponse("SparkStatusResponse", ExternalStatus.RUNNING.name(), null, "driver-abc-123", true, null, null, null));

        SubmissionStatusResponse response = service.getStatus(1L);
        assertEquals(ExternalStatus.RUNNING.name(), response.message());
        verify(adapter).getSubmissionStatus(eq("driver-abc-123"));
    }

    // --- kill ---

    @Test
    void kill_notFound_returnsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        CreateSubmissionResponse response = service.kill(99L);
        assertEquals("NOT_FOUND", response.message());
        assertFalse(response.success());
    }

    @Test
    void kill_queuedTask_deletesFromDb() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.QUEUED);
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        CreateSubmissionResponse response = service.kill(1L);
        assertEquals(ExternalStatus.KILLED.name(), response.message());
        assertTrue(response.success());
        verify(repository).delete(sub);
    }

    @Test
    void kill_claimedTask_deletesFromDb() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setStatus(SparkSubmission.Status.CLAIMED);
        when(repository.findById(1L)).thenReturn(Optional.of(sub));

        service.kill(1L);
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
                .thenReturn(new CreateSubmissionResponse("KillResponse", ExternalStatus.KILLED.name(), null, "driver-abc-123", true));

        CreateSubmissionResponse response = service.kill(1L);
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
                .thenReturn(new CreateSubmissionResponse("KillResponse", ExternalStatus.FAILED.name(), null, "driver-abc-123", false));

        service.kill(1L);
        verify(repository, never()).delete(any());
    }

    // --- killAll ---

    @Test
    void killAll_deletesQueuedAndClaimed() {
        SparkSubmission q1 = new SparkSubmission();
        q1.setId(1L);
        SparkSubmission q2 = new SparkSubmission();
        q2.setId(2L);
        SparkSubmission c1 = new SparkSubmission();
        c1.setId(3L);

        when(repository.findByStatus(SparkSubmission.Status.QUEUED)).thenReturn(List.of(q1, q2));
        when(repository.findByStatus(SparkSubmission.Status.CLAIMED)).thenReturn(List.of(c1));

        CreateSubmissionResponse response = service.killAll();
        assertTrue(response.success());
        assertTrue(response.message().contains("3"));
        verify(repository).delete(q1);
        verify(repository).delete(q2);
        verify(repository).delete(c1);
    }

    @Test
    void killAll_noTasks() {
        when(repository.findByStatus(SparkSubmission.Status.QUEUED)).thenReturn(List.of());
        when(repository.findByStatus(SparkSubmission.Status.CLAIMED)).thenReturn(List.of());

        CreateSubmissionResponse response = service.killAll();
        assertTrue(response.success());
        assertTrue(response.message().contains("0"));
    }

    // --- clear ---

    @Test
    void clear_deletesCompletedSubmissions() {
        SparkSubmission sub1 = new SparkSubmission();
        sub1.setId(1L);
        sub1.setSubmissionId("driver-abc-123");

        when(repository.findByStatus(SparkSubmission.Status.COMPLETED)).thenReturn(List.of(sub1));

        CreateSubmissionResponse response = service.clear();
        assertTrue(response.success());
        assertTrue(response.message().contains("1"));
        verify(adapter).clearCompleted();
        verify(repository).delete(sub1);
    }

    @Test
    void clear_noCompletedSubmissions() {
        when(repository.findByStatus(SparkSubmission.Status.COMPLETED)).thenReturn(List.of());

        CreateSubmissionResponse response = service.clear();
        assertTrue(response.success());
        assertTrue(response.message().contains("0"));
    }

    @Test
    void clear_submissionWithNullId_stillCallsAdapter() {
        SparkSubmission sub = new SparkSubmission();
        sub.setId(1L);
        sub.setSubmissionId(null);

        when(repository.findByStatus(SparkSubmission.Status.COMPLETED)).thenReturn(List.of(sub));

        service.clear();
        verify(adapter).clearCompleted();
        verify(repository).delete(sub);
    }
}
