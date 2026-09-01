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

package org.lakehouse.config.vcs.component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lakehouse.config.vcs.VcsClient;
import org.lakehouse.config.vcs.VcsClientException;
import org.lakehouse.config.vcs.entity.VcsSyncLog;
import org.lakehouse.config.vcs.entity.VcsSyncStatus;
import org.lakehouse.config.vcs.repository.VcsSyncLogRepository;
import org.lakehouse.config.vcs.service.GitOpsChangeSetBuilder;
import org.lakehouse.config.vcs.service.GitOpsFailureRecorder;
import org.lakehouse.config.vcs.service.GitOpsSynchronizer;
import org.lakehouse.config.vcs.service.GitSyncChangeSet;
import org.lakehouse.config.vcs.yaml.VcsConfigParseException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitOpsSchedulerUnitTest {

    private static final String HEAD = "a".repeat(40);
    private static final String BASE = "b".repeat(40);

    @Mock
    private VcsClient vcsClient;
    @Mock
    private GitOpsChangeSetBuilder changeSetBuilder;
    @Mock
    private GitOpsSynchronizer synchronizer;
    @Mock
    private GitOpsFailureRecorder failureRecorder;
    @Mock
    private VcsSyncLogRepository vcsSyncLogRepository;

    @InjectMocks
    private GitOpsScheduler scheduler;

    @Test
    void skipsCommitThatWasAlreadyProcessed() {
        when(vcsSyncLogRepository.existsByCommitId(HEAD)).thenReturn(true);

        scheduler.sync();

        verify(changeSetBuilder, never()).build(any(), any());
        verify(synchronizer, never()).sync(any(), any());
        verify(failureRecorder, never()).recordFailure(any(), any());
    }

    @Test
    void skipsWhenHeadEqualsLastSuccess() {
        when(vcsSyncLogRepository.existsByCommitId(HEAD)).thenReturn(false);
        when(vcsSyncLogRepository.findFirstByStatusOrderBySyncDateTimeDesc(VcsSyncStatus.SUCCESS))
                .thenReturn(Optional.of(log(HEAD)));

        scheduler.sync();

        verify(changeSetBuilder, never()).build(any(), any());
        verify(synchronizer, never()).sync(any(), any());
    }

    @Test
    void appliesChangeSetAgainstLastSuccessBase() {
        when(vcsClient.getCurrentCommitId()).thenReturn(HEAD);
        when(vcsSyncLogRepository.existsByCommitId(HEAD)).thenReturn(false);
        when(vcsSyncLogRepository.findFirstByStatusOrderBySyncDateTimeDesc(VcsSyncStatus.SUCCESS))
                .thenReturn(Optional.of(log(BASE)));
        GitSyncChangeSet changeSet = changeSet();
        when(changeSetBuilder.build(HEAD, BASE)).thenReturn(changeSet);

        scheduler.sync();

        verify(synchronizer).sync(changeSet, HEAD);
        verify(failureRecorder, never()).recordFailure(any(), any());
    }

    @Test
    void appliesChangeSetAgainstNullBaseWhenNeverSucceeded() {
        when(vcsClient.getCurrentCommitId()).thenReturn(HEAD);
        when(vcsSyncLogRepository.existsByCommitId(HEAD)).thenReturn(false);
        when(vcsSyncLogRepository.findFirstByStatusOrderBySyncDateTimeDesc(VcsSyncStatus.SUCCESS))
                .thenReturn(Optional.empty());
        GitSyncChangeSet changeSet = changeSet();
        when(changeSetBuilder.build(HEAD, null)).thenReturn(changeSet);

        scheduler.sync();

        verify(synchronizer).sync(changeSet, HEAD);
    }

    @Test
    void recordsFailureWhenApplyingFails() {
        when(vcsClient.getCurrentCommitId()).thenReturn(HEAD);
        when(vcsSyncLogRepository.existsByCommitId(HEAD)).thenReturn(false);
        when(vcsSyncLogRepository.findFirstByStatusOrderBySyncDateTimeDesc(VcsSyncStatus.SUCCESS))
                .thenReturn(Optional.empty());
        GitSyncChangeSet changeSet = changeSet();
        when(changeSetBuilder.build(HEAD, null)).thenReturn(changeSet);
        doThrow(new RuntimeException("commit rejected")).when(synchronizer).sync(changeSet, HEAD);

        scheduler.sync();

        verify(failureRecorder).recordFailure(HEAD, "commit rejected");
    }

    @Test
    void recordsFailureWhenBuildingChangeSetFails() {
        when(vcsClient.getCurrentCommitId()).thenReturn(HEAD);
        when(vcsSyncLogRepository.existsByCommitId(HEAD)).thenReturn(false);
        when(vcsSyncLogRepository.findFirstByStatusOrderBySyncDateTimeDesc(VcsSyncStatus.SUCCESS))
                .thenReturn(Optional.empty());
        when(changeSetBuilder.build(any(), any()))
                .thenThrow(new VcsConfigParseException("cannot parse config file"));

        scheduler.sync();

        verify(failureRecorder).recordFailure(HEAD, "cannot parse config file");
        verify(synchronizer, never()).sync(any(), any());
    }

    @Test
    void infrastructureFailureIsRetriedWithoutRecord() {
        doThrow(new VcsClientException("origin unreachable")).when(vcsClient).pull();

        scheduler.sync();

        verify(failureRecorder, never()).recordFailure(any(), any());
        verify(synchronizer, never()).sync(any(), any());
        verify(changeSetBuilder, never()).build(any(), any());
    }

    @Test
    void initializesTheClientOnlyOnce() {
        when(vcsSyncLogRepository.existsByCommitId(HEAD)).thenReturn(true);

        scheduler.sync();
        scheduler.sync();

        verify(vcsClient, times(1)).init();
        verify(vcsClient, times(2)).pull();
    }

    private GitSyncChangeSet changeSet() {
        return new GitSyncChangeSet(List.of(), List.of());
    }

    private VcsSyncLog log(String commitId) {
        return new VcsSyncLog(commitId, OffsetDateTime.now(), VcsSyncStatus.SUCCESS, null);
    }
}