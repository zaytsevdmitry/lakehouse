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
import org.lakehouse.config.vcs.CurrentDomainContext;
import org.lakehouse.config.vcs.LakehouseVCSProperties;
import org.lakehouse.config.vcs.VcsClient;
import org.lakehouse.config.vcs.VcsClientException;
import org.lakehouse.config.vcs.configuration.GitVcsClientFactory;
import org.lakehouse.config.vcs.entity.VcsSyncLog;
import org.lakehouse.config.vcs.entity.VcsSyncStatus;
import org.lakehouse.config.vcs.repository.VcsSyncLogRepository;
import org.lakehouse.config.vcs.service.GitOpsChangeSetBuilder;
import org.lakehouse.config.vcs.service.GitOpsFailureRecorder;
import org.lakehouse.config.vcs.service.GitOpsSynchronizer;
import org.lakehouse.config.vcs.service.GitSyncChangeSet;
import org.lakehouse.config.vcs.yaml.VcsConfigParseException;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitOpsSchedulerUnitTest {

    private static final String HEAD = "a".repeat(40);
    private static final String BASE = "b".repeat(40);

    @Mock
    private GitVcsClientFactory vcsClientFactory;
    @Mock
    private GitOpsChangeSetBuilder changeSetBuilder;
    @Mock
    private GitOpsSynchronizer synchronizer;
    @Mock
    private GitOpsFailureRecorder failureRecorder;
    @Mock
    private VcsSyncLogRepository vcsSyncLogRepository;
    @Mock
    private VcsClient vcsClient;

    private final CurrentDomainContext domainContext = new CurrentDomainContext();

    @InjectMocks
    private GitOpsScheduler scheduler;

    @Test
    void traversesDomainsInPriorityOrderWithinHierarchy() {
        LakehouseVCSProperties.DomainProperties platform = d(0, git());
        LakehouseVCSProperties.DomainProperties pocessing = d(1, git());
        LakehouseVCSProperties.DomainProperties analytics = d(2, git());
        pocessing.setDomains(orderedNamed("analytics", analytics));
        platform.setDomains(orderedNamed("pocessing", pocessing));
        scheduler = schedulerWith(props(orderedNamed("platform", platform)));
        when(vcsClientFactory.create(any(), any())).thenReturn(vcsClient);

        scheduler.sync();

        ArgumentCaptor<String> domainNames = ArgumentCaptor.forClass(String.class);
        verify(vcsClientFactory, org.mockito.Mockito.times(3)).create(domainNames.capture(), any());
        assertThat(domainNames.getAllValues()).containsExactly("platform", "pocessing", "analytics");
    }

    @Test
    void skipsDomainWithoutConfiguredRepository() {
        scheduler = schedulerWith(props(orderedNamed(
                "platform", d(0, null),
                "analytics", d(1, git()))));

        scheduler.sync();

        verify(vcsClientFactory).create(eq("analytics"), any());
        verify(vcsClientFactory, org.mockito.Mockito.never()).create(eq("platform"), any());
    }

    @Test
    void skipsCommitAlreadyProcessedForDomain() {
        when(vcsClientFactory.create(any(), any())).thenReturn(vcsClient);
        when(vcsClient.getCurrentCommitId()).thenReturn(HEAD);
        when(vcsSyncLogRepository.existsByCommitIdAndDomainKeyName(HEAD, "platform")).thenReturn(true);
        scheduler = schedulerWith(props(orderedNamed("platform", d(0, git()))));

        scheduler.sync();

        verify(changeSetBuilder, never()).build(any(), any(), any());
        verify(synchronizer, never()).sync(any(), any());
        verify(failureRecorder, never()).recordFailure(any(), any(), any());
    }

    @Test
    void skipsWhenHeadEqualsLastSuccessForDomain() {
        when(vcsClientFactory.create(any(), any())).thenReturn(vcsClient);
        when(vcsClient.getCurrentCommitId()).thenReturn(HEAD);
        when(vcsSyncLogRepository.existsByCommitIdAndDomainKeyName(HEAD, "platform")).thenReturn(false);
        when(vcsSyncLogRepository.findFirstByStatusAndDomainKeyNameOrderBySyncDateTimeDesc(VcsSyncStatus.SUCCESS, "platform"))
                .thenReturn(Optional.of(log(HEAD)));
        scheduler = schedulerWith(props(orderedNamed(
                "platform", d(0, git()))));

        scheduler.sync();

        verify(changeSetBuilder, never()).build(any(), any(), any());
        verify(synchronizer, never()).sync(any(), any());
    }

    @Test
    void appliesChangeSetAgainstLastSuccessBaseForDomain() {
        when(vcsClientFactory.create(any(), any())).thenReturn(vcsClient);
        when(vcsClient.getCurrentCommitId()).thenReturn(HEAD);
        when(vcsSyncLogRepository.existsByCommitIdAndDomainKeyName(HEAD, "platform")).thenReturn(false);
        when(vcsSyncLogRepository.findFirstByStatusAndDomainKeyNameOrderBySyncDateTimeDesc(VcsSyncStatus.SUCCESS, "platform"))
                .thenReturn(Optional.of(log(BASE)));
        GitSyncChangeSet changeSet = changeSet();
        when(changeSetBuilder.build(vcsClient, HEAD, BASE)).thenReturn(changeSet);
        scheduler = schedulerWith(props(orderedNamed(
                "platform", d(0, git()))));

        scheduler.sync();

        verify(synchronizer).sync(changeSet, HEAD);
        verify(failureRecorder, never()).recordFailure(any(), any(), any());
    }

    @Test
    void appliesChangeSetAgainstNullBaseWhenNeverSucceededForDomain() {
        when(vcsClientFactory.create(any(), any())).thenReturn(vcsClient);
        when(vcsClient.getCurrentCommitId()).thenReturn(HEAD);
        when(vcsSyncLogRepository.existsByCommitIdAndDomainKeyName(HEAD, "platform")).thenReturn(false);
        when(vcsSyncLogRepository.findFirstByStatusAndDomainKeyNameOrderBySyncDateTimeDesc(VcsSyncStatus.SUCCESS, "platform"))
                .thenReturn(Optional.empty());
        GitSyncChangeSet changeSet = changeSet();
        when(changeSetBuilder.build(vcsClient, HEAD, null)).thenReturn(changeSet);
        scheduler = schedulerWith(props(orderedNamed(
                "platform", d(0, git()))));

        scheduler.sync();

        verify(synchronizer).sync(changeSet, HEAD);
    }

    @Test
    void recordsFailureWhenApplyingFailsForDomain() {
        when(vcsClientFactory.create(any(), any())).thenReturn(vcsClient);
        when(vcsClient.getCurrentCommitId()).thenReturn(HEAD);
        when(vcsSyncLogRepository.existsByCommitIdAndDomainKeyName(HEAD, "platform")).thenReturn(false);
        when(vcsSyncLogRepository.findFirstByStatusAndDomainKeyNameOrderBySyncDateTimeDesc(VcsSyncStatus.SUCCESS, "platform"))
                .thenReturn(Optional.empty());
        GitSyncChangeSet changeSet = changeSet();
        when(changeSetBuilder.build(vcsClient, HEAD, null)).thenReturn(changeSet);
        doThrow(new RuntimeException("commit rejected")).when(synchronizer).sync(changeSet, HEAD);
        scheduler = schedulerWith(props(orderedNamed(
                "platform", d(0, git()))));

        scheduler.sync();

        verify(failureRecorder).recordFailure("platform", HEAD, "commit rejected");
    }

    @Test
    void recordsFailureWhenBuildingChangeSetFailsForDomain() {
        when(vcsClientFactory.create(any(), any())).thenReturn(vcsClient);
        when(vcsClient.getCurrentCommitId()).thenReturn(HEAD);
        when(vcsSyncLogRepository.existsByCommitIdAndDomainKeyName(HEAD, "platform")).thenReturn(false);
        when(vcsSyncLogRepository.findFirstByStatusAndDomainKeyNameOrderBySyncDateTimeDesc(VcsSyncStatus.SUCCESS, "platform"))
                .thenReturn(Optional.empty());
        when(changeSetBuilder.build(any(), any(), any()))
                .thenThrow(new VcsConfigParseException("cannot parse config file"));
        scheduler = schedulerWith(props(orderedNamed(
                "platform", d(0, git()))));

        scheduler.sync();

        verify(failureRecorder).recordFailure("platform", HEAD, "cannot parse config file");
        verify(synchronizer, never()).sync(any(), any());
    }

    @Test
    void infrastructureFailureIsRetriedWithoutRecord() {
        when(vcsClientFactory.create(any(), any())).thenReturn(vcsClient);
        doThrow(new VcsClientException("origin unreachable")).when(vcsClient).pull();
        scheduler = schedulerWith(props(orderedNamed(
                "platform", d(0, git()))));

        scheduler.sync();

        verify(failureRecorder, never()).recordFailure(any(), any(), any());
        verify(synchronizer, never()).sync(any(), any());
    }

    @Test
    void skipsNestedDomainsWhenParentInfrastructureFails() {
        LakehouseVCSProperties.DomainProperties platform = d(0, git());
        LakehouseVCSProperties.DomainProperties pocessing = d(1, git());
        platform.setDomains(orderedNamed("pocessing", pocessing));
        scheduler = schedulerWith(props(orderedNamed("platform", platform)));
        when(vcsClientFactory.create(any(), any())).thenReturn(vcsClient);
        doThrow(new VcsClientException("origin unreachable")).when(vcsClient).pull();

        scheduler.sync();

        verify(vcsClientFactory).create(eq("platform"), any());
        verify(vcsClientFactory, never()).create(eq("pocessing"), any());
    }

    @Test
    void skipsNestedDomainsWhenParentApplyFails() {
        LakehouseVCSProperties.DomainProperties platform = d(0, git());
        LakehouseVCSProperties.DomainProperties pocessing = d(1, git());
        platform.setDomains(orderedNamed("pocessing", pocessing));
        scheduler = schedulerWith(props(orderedNamed("platform", platform)));
        when(vcsClientFactory.create(any(), any())).thenReturn(vcsClient);
        when(vcsClient.getCurrentCommitId()).thenReturn(HEAD);
        when(vcsSyncLogRepository.existsByCommitIdAndDomainKeyName(HEAD, "platform")).thenReturn(false);
        when(vcsSyncLogRepository.findFirstByStatusAndDomainKeyNameOrderBySyncDateTimeDesc(VcsSyncStatus.SUCCESS, "platform"))
                .thenReturn(Optional.empty());
        when(changeSetBuilder.build(vcsClient, HEAD, null)).thenReturn(changeSet());
        doThrow(new RuntimeException("commit rejected")).when(synchronizer).sync(any(), any());

        scheduler.sync();

        verify(failureRecorder).recordFailure("platform", HEAD, "commit rejected");
        verify(vcsClientFactory, never()).create(eq("pocessing"), any());
    }

    @Test
    void processesNestedDomainsWhenParentHasNothingToSync() {
        LakehouseVCSProperties.DomainProperties platform = d(0, git());
        LakehouseVCSProperties.DomainProperties pocessing = d(1, git());
        platform.setDomains(orderedNamed("pocessing", pocessing));
        scheduler = schedulerWith(props(orderedNamed("platform", platform)));
        when(vcsClientFactory.create(any(), any())).thenReturn(vcsClient);
        when(vcsClient.getCurrentCommitId()).thenReturn(HEAD);
        when(vcsSyncLogRepository.existsByCommitIdAndDomainKeyName(HEAD, "platform")).thenReturn(true);

        scheduler.sync();

        ArgumentCaptor<String> domainNames = ArgumentCaptor.forClass(String.class);
        verify(vcsClientFactory, org.mockito.Mockito.times(2)).create(domainNames.capture(), any());
        assertThat(domainNames.getAllValues()).containsExactly("platform", "pocessing");
    }

    @Test
    void domainContextIsSetDuringSyncAndClearedAfterwards() {
        when(vcsClientFactory.create(any(), any())).thenReturn(vcsClient);
        when(vcsClient.getCurrentCommitId()).thenReturn(HEAD);
        when(vcsSyncLogRepository.existsByCommitIdAndDomainKeyName(HEAD, "platform")).thenReturn(false);
        when(vcsSyncLogRepository.findFirstByStatusAndDomainKeyNameOrderBySyncDateTimeDesc(VcsSyncStatus.SUCCESS, "platform"))
                .thenReturn(Optional.empty());
        when(changeSetBuilder.build(vcsClient, HEAD, null)).thenReturn(changeSet());
        org.mockito.Mockito.doAnswer(invocation -> {
            assertThat(domainContext.get()).isEqualTo("platform");
            return null;
        }).when(synchronizer).sync(any(), any());
        scheduler = schedulerWith(props(orderedNamed(
                "platform", d(0, git()))));

        scheduler.sync();

        assertThat(domainContext.get()).isNull();
        verify(synchronizer).sync(any(), any());
    }

    @Test
    void closesTheClientOfEveryProcessedDomain() {
        LakehouseVCSProperties.DomainProperties platform = d(0, git());
        LakehouseVCSProperties.DomainProperties pocessing = d(1, git());
        platform.setDomains(orderedNamed("pocessing", pocessing));
        scheduler = schedulerWith(props(orderedNamed("platform", platform)));
        when(vcsClientFactory.create(any(), any())).thenReturn(vcsClient);
        when(vcsClient.getCurrentCommitId()).thenReturn(HEAD);
        when(vcsSyncLogRepository.existsByCommitIdAndDomainKeyName(any(), any())).thenReturn(true);

        scheduler.sync();

        verify(vcsClient, org.mockito.Mockito.times(2)).close();
    }

    @Test
    void closesTheClientWhenTheDomainFails() {
        when(vcsClientFactory.create(any(), any())).thenReturn(vcsClient);
        doThrow(new VcsClientException("origin unreachable")).when(vcsClient).pull();
        scheduler = schedulerWith(props(orderedNamed("platform", d(0, git()))));

        scheduler.sync();

        verify(vcsClient).close();
        verify(failureRecorder, never()).recordFailure(any(), any(), any());
    }

    @Test
    void syncsTheLegacyRepositoryAsTheDefaultDomain() {
        LakehouseVCSProperties properties = new LakehouseVCSProperties();
        properties.getGit().setRepositoryUrl("https://git.example/conf.git");
        scheduler = schedulerWith(properties);
        when(vcsClientFactory.create(any(), any())).thenReturn(vcsClient);
        when(vcsClient.getCurrentCommitId()).thenReturn(HEAD);
        when(vcsSyncLogRepository.existsByCommitIdAndDomainKeyName(HEAD, "default")).thenReturn(false);
        when(vcsSyncLogRepository.findFirstByStatusAndDomainKeyNameOrderBySyncDateTimeDesc(VcsSyncStatus.SUCCESS, "default"))
                .thenReturn(Optional.empty());
        when(changeSetBuilder.build(vcsClient, HEAD, null)).thenReturn(changeSet());

        scheduler.sync();

        verify(vcsClientFactory).create(eq("default"), any());
        verify(synchronizer).sync(any(), eq(HEAD));
    }

    private GitOpsScheduler schedulerWith(LakehouseVCSProperties properties) {
        return new GitOpsScheduler(
                properties,
                vcsClientFactory,
                domainContext,
                changeSetBuilder,
                synchronizer,
                failureRecorder,
                vcsSyncLogRepository);
    }

    private LakehouseVCSProperties props(Map<String, LakehouseVCSProperties.DomainProperties> domains) {
        LakehouseVCSProperties properties = new LakehouseVCSProperties();
        properties.setDomains(domains);
        return properties;
    }

    private java.util.LinkedHashMap<String, LakehouseVCSProperties.DomainProperties> orderedNamed(Object... nameAndValue) {
        java.util.LinkedHashMap<String, LakehouseVCSProperties.DomainProperties> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < nameAndValue.length; i += 2) {
            if (nameAndValue[i + 1] != null)
                map.put((String) nameAndValue[i], (LakehouseVCSProperties.DomainProperties) nameAndValue[i + 1]);
        }
        return map;
    }

    private LakehouseVCSProperties.DomainProperties d(int priority, LakehouseVCSProperties.GitProperties git) {
        LakehouseVCSProperties.DomainProperties d = new LakehouseVCSProperties.DomainProperties();
        d.setPriority(priority);
        d.setGit(git);
        return d;
    }

    private LakehouseVCSProperties.GitProperties git() {
        LakehouseVCSProperties.GitProperties git = new LakehouseVCSProperties.GitProperties();
        git.setRepositoryUrl("https://git.example/conf.git");
        return git;
    }

    private GitSyncChangeSet changeSet() {
        return new GitSyncChangeSet(List.of(), List.of());
    }

    private VcsSyncLog log(String commitId) {
        return new VcsSyncLog(commitId, OffsetDateTime.now(), VcsSyncStatus.SUCCESS, null);
    }
}