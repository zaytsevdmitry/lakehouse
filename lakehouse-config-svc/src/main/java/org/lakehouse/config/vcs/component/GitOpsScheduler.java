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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Periodically synchronizes every configured configuration domain.
 * <p>
 * Domains are traversed strictly in the priority order (ascending) of the configuration
 * hierarchy, parent domains before their nested ones, one thread, sequential transactions:
 * each domain repository is pulled, diffed and applied inside its own transaction. All
 * changes belonging to a single commit of a single domain are applied atomically; a
 * configuration error rolls the whole commit back and is recorded in the synchronization
 * log as FAILED for that domain.
 * <p>
 * Sub-domains have a hierarchical dependency on their parent domain: when synchronization
 * of a domain fails, its whole sub-tree is skipped until the parent succeeds again.
 */
@Component
@ConditionalOnProperty(prefix = "lakehouse.config.vcs.git.sync", name = "enabled", havingValue = "true")
public class GitOpsScheduler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final LakehouseVCSProperties lakehouseVCSProperties;
    private final GitVcsClientFactory vcsClientFactory;
    private final CurrentDomainContext domainContext;
    private final GitOpsChangeSetBuilder changeSetBuilder;
    private final GitOpsSynchronizer synchronizer;
    private final GitOpsFailureRecorder failureRecorder;
    private final VcsSyncLogRepository vcsSyncLogRepository;

    public GitOpsScheduler(
            LakehouseVCSProperties lakehouseVCSProperties,
            GitVcsClientFactory vcsClientFactory,
            CurrentDomainContext domainContext,
            GitOpsChangeSetBuilder changeSetBuilder,
            GitOpsSynchronizer synchronizer,
            GitOpsFailureRecorder failureRecorder,
            VcsSyncLogRepository vcsSyncLogRepository) {
        this.lakehouseVCSProperties = lakehouseVCSProperties;
        this.vcsClientFactory = vcsClientFactory;
        this.domainContext = domainContext;
        this.changeSetBuilder = changeSetBuilder;
        this.synchronizer = synchronizer;
        this.failureRecorder = failureRecorder;
        this.vcsSyncLogRepository = vcsSyncLogRepository;
    }

    /**
     * Synchronizes all configuration domains. The method may be invoked both by the
     * scheduler and directly, e.g. by integration tests, so it is safe to call it
     * repeatedly.
     */
    @Scheduled(
            fixedDelayString = "${lakehouse.config.vcs.git.sync.interval-ms}",
            initialDelayString = "${lakehouse.config.vcs.git.sync.initial-delay-ms}")
    public synchronized void sync() {
        for (LakehouseVCSProperties.DomainRef domain : lakehouseVCSProperties.rootDomains())
            syncDomainTree(domain);
    }

    /**
     * Synchronizes a domain and, only when it succeeds, its nested sub-domains. When
     * processing of a domain fails, its whole sub-tree is skipped: sub-domains have a
     * hierarchical dependency on the parent domain and are unreliable on top of a parent
     * that did not apply.
     */
    private void syncDomainTree(LakehouseVCSProperties.DomainRef domain) {
        if (!syncDomain(domain))
            return;
        for (LakehouseVCSProperties.DomainRef nested : lakehouseVCSProperties.nestedDomains(domain.properties()))
            syncDomainTree(nested);
    }

    /**
     * @return {@code true} when the domain was successfully synchronized (or there was
     * nothing to do); {@code false} when processing of the domain failed, in which case
     * its sub-domains must not be processed.
     */
    private boolean syncDomain(LakehouseVCSProperties.DomainRef domain) {
        String domainName = domain.name();
        LakehouseVCSProperties.DomainProperties properties = domain.properties();
        if (!properties.isRepositoryConfigured()) {
            logger.warn("Domain {} has no VCS repository configured, skipping", domainName);
            return true;
        }
        try (VcsClient vcsClient = vcsClientFactory.create(domainName, properties)) {
            vcsClient.init();
            vcsClient.pull();
            String head = vcsClient.getCurrentCommitId();
            if (head == null || head.isBlank())
                return true;

            if (vcsSyncLogRepository.existsByCommitIdAndDomainKeyName(head, domainName)) {
                logger.debug("Commit {} of domain {} already processed, skipping", head, domainName);
                return true;
            }

            Optional<VcsSyncLog> lastSuccess = vcsSyncLogRepository
                    .findFirstByStatusAndDomainKeyNameOrderBySyncDateTimeDesc(VcsSyncStatus.SUCCESS, domainName);
            if (lastSuccess.isPresent() && lastSuccess.get().getCommitId().equals(head)) {
                logger.debug("Nothing to sync; head {} of domain {} already applied", head, domainName);
                return true;
            }

            String base = lastSuccess.map(VcsSyncLog::getCommitId).orElse(null);
            domainContext.set(domainName);
            try {
                GitSyncChangeSet changeSet = changeSetBuilder.build(vcsClient, head, base);
                synchronizer.sync(changeSet, head);
            } catch (Exception e) {
                failureRecorder.recordFailure(domainName, head, errorMessage(e));
                return false;
            } finally {
                domainContext.clear();
            }
            return true;
        } catch (VcsClientException e) {
            logger.warn("VCS infrastructure failure for domain {}, will retry: {}", domainName, e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Unexpected failure for domain {} during configuration synchronization", domainName, e);
            return false;
        }
    }

    private String errorMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}