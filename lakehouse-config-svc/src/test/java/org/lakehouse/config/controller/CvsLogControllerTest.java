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

package org.lakehouse.config.controller;

import org.junit.jupiter.api.Test;
import org.lakehouse.client.api.dto.configs.CvsObjectLogDTO;
import org.lakehouse.client.api.dto.configs.CvsSyncLogDTO;
import org.lakehouse.config.cvs.service.CvsObjectLogService;
import org.lakehouse.config.cvs.service.CvsSyncLogService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CvsLogControllerTest {

    private final CvsSyncLogService syncLogService = mock(CvsSyncLogService.class);
    private final CvsObjectLogService objectLogService = mock(CvsObjectLogService.class);
    private final CvsSyncLogController syncLogController = new CvsSyncLogController(syncLogService);
    private final CvsObjectLogController objectLogController = new CvsObjectLogController(objectLogService);

    @Test
    void syncLogFindDelegatesToServiceWithAllFilters() {
        OffsetDateTime from = OffsetDateTime.now().minusDays(1);
        OffsetDateTime to = OffsetDateTime.now();
        CvsSyncLogDTO log = new CvsSyncLogDTO();
        log.setId(1L);
        log.setCommitId("abc123");
        log.setSyncDateTime(to);
        log.setStatus("SUCCESS");
        when(syncLogService.find(from, to, "SUCCESS", "abc123")).thenReturn(List.of(log));

        List<CvsSyncLogDTO> result = syncLogController.find(from, to, "SUCCESS", "abc123");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCommitId()).isEqualTo("abc123");
        verify(syncLogService).find(from, to, "SUCCESS", "abc123");
    }

    @Test
    void objectLogFindByCommitIdDelegatesToService() {
        CvsObjectLogDTO obj = new CvsObjectLogDTO();
        obj.setId(10L);
        obj.setObjectName("dataset/transaction_dds");
        obj.setKind("dataSet");
        obj.setCommitId("abc123");
        when(objectLogService.find("abc123", null, null, null, null, null))
                .thenReturn(List.of(obj));

        List<CvsObjectLogDTO> result = objectLogController.find("abc123", null, null, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getObjectName()).isEqualTo("dataset/transaction_dds");
        verify(objectLogService).find("abc123", null, null, null, null, null);
    }

    @Test
    void objectLogFindByIntervalDelegatesToService() {
        OffsetDateTime from = OffsetDateTime.now().minusDays(1);
        OffsetDateTime to = OffsetDateTime.now();
        when(objectLogService.find(null, "dataSet", from, to, "dir/", "transaction"))
                .thenReturn(List.of());

        List<CvsObjectLogDTO> result = objectLogController.find(null, "dataSet", from, to, "dir/", "transaction");

        assertThat(result).isEmpty();
        verify(objectLogService).find(null, "dataSet", from, to, "dir/", "transaction");
    }

    @Test
    void objectLogFindRejectsNeitherCommitNorInterval() {
        assertThatThrownBy(() -> objectLogController.find(null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("commitId");
    }
}
