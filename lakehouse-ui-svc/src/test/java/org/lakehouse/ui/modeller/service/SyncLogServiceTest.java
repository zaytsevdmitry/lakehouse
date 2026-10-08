/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.ui.modeller.service;

import org.junit.jupiter.api.Test;
import org.lakehouse.ui.modeller.dto.SyncLogResponse;

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