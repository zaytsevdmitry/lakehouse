package org.lakehouse.modeller.service;

import org.junit.jupiter.api.Test;
import org.lakehouse.modeller.dto.SyncLogResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SyncLogServiceTest {

    @Test
    void capacityIsClampedToAtLeastOne() {
        assertThat(new SyncLogService(-5).capacity()).isEqualTo(1);
        assertThat(new SyncLogService(0).capacity()).isEqualTo(1);
        assertThat(new SyncLogService(10).capacity()).isEqualTo(10);
    }

    @Test
    void logsAreReturnedNewestLastInInsertionOrder() {
        SyncLogService service = new SyncLogService(10);
        service.log("INFO", "alice", "review", "submitted", "ws1");
        service.log("INFO", "bob", "review", "approved", "ws2");

        List<SyncLogResponse> latest = service.latest(10);
        assertThat(latest).hasSize(2);
        assertThat(latest.get(0).user()).isEqualTo("alice");
        assertThat(latest.get(1).user()).isEqualTo("bob");
        assertThat(latest.get(0).workspaceId()).isEqualTo("ws1");
    }

    @Test
    void bufferWrapsAroundWhenCapacityIsExceeded() {
        SyncLogService service = new SyncLogService(3);
        for (int i = 1; i <= 5; i++)
            service.log("INFO", "user" + i, "action", "entry " + i, null);

        List<SyncLogResponse> latest = service.latest(10);
        assertThat(latest).hasSize(3);
        assertThat(latest.get(0).user()).isEqualTo("user3");
        assertThat(latest.get(2).user()).isEqualTo("user5");
    }

    @Test
    void latestCapsTheResultAndClampsTheLimit() {
        SyncLogService service = new SyncLogService(4);
        for (int i = 1; i <= 4; i++)
            service.log("INFO", "user" + i, "action", "entry " + i, null);

        assertThat(service.latest(2)).hasSize(2);
        assertThat(service.latest(0)).hasSize(1);
        assertThat(service.latest(-3)).hasSize(1);
        assertThat(service.latest(99)).hasSize(4);
    }
}