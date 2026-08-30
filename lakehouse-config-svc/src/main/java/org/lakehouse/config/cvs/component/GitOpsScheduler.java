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

package org.lakehouse.config.cvs.component;

import org.lakehouse.config.cvs.CvsClient;
import org.lakehouse.config.cvs.CvsClientException;
import org.lakehouse.config.cvs.entity.CvsSyncLog;
import org.lakehouse.config.cvs.entity.CvsSyncStatus;
import org.lakehouse.config.cvs.repository.CvsSyncLogRepository;
import org.lakehouse.config.cvs.service.GitOpsChangeSetBuilder;
import org.lakehouse.config.cvs.service.GitOpsFailureRecorder;
import org.lakehouse.config.cvs.service.GitOpsSynchronizer;
import org.lakehouse.config.cvs.service.GitSyncChangeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Periodically synchronizes the declarative configuration repository with the database.
 * <p>
 * Each cycle: fetches the configured branch, diffs its head against the last successfully
 * applied commit, parses the changed YAML files and applies them inside a single
 * transaction. Configuration errors are recorded as FAILED synchronization log entries,
 * while infrastructure errors (unreachable repository and the like) are left to be retried
 * on the next cycle.
 */
@Component
@ConditionalOnProperty(prefix = "lakehouse.config.cvs.git.sync", name = "enabled", havingValue = "true")
public class GitOpsScheduler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final CvsClient cvsClient;
    private final GitOpsChangeSetBuilder changeSetBuilder;
    private final GitOpsSynchronizer synchronizer;
    private final GitOpsFailureRecorder failureRecorder;
    private final CvsSyncLogRepository cvsSyncLogRepository;

    private boolean initialized;

    public GitOpsScheduler(
            CvsClient cvsClient,
            GitOpsChangeSetBuilder changeSetBuilder,
            GitOpsSynchronizer synchronizer,
            GitOpsFailureRecorder failureRecorder,
            CvsSyncLogRepository cvsSyncLogRepository) {
        this.cvsClient = cvsClient;
        this.changeSetBuilder = changeSetBuilder;
        this.synchronizer = synchronizer;
        this.failureRecorder = failureRecorder;
        this.cvsSyncLogRepository = cvsSyncLogRepository;
    }

    /**
     * Synchronizes the configuration. The method may be invoked both by the scheduler
     * and directly, e.g. by integration tests, so it is safe to call it repeatedly.
     */
    @Scheduled(
            fixedDelayString = "${lakehouse.config.cvs.git.sync.interval-ms}",
            initialDelayString = "${lakehouse.config.cvs.git.sync.initial-delay-ms}")
    public synchronized void sync() {
        String head = null;
        try {
            initClient();
            cvsClient.pull();
            head = cvsClient.getCurrentCommitId();

            if (cvsSyncLogRepository.existsByCommitId(head)) {
                logger.debug("Commit {} already processed, skipping", head);
                return;
            }

            Optional<CvsSyncLog> lastSuccess = cvsSyncLogRepository.findFirstByStatusOrderBySyncDateTimeDesc(CvsSyncStatus.SUCCESS);
            if (lastSuccess.isPresent() && lastSuccess.get().getCommitId().equals(head)) {
                logger.debug("Nothing to sync; head {} already applied", head);
                return;
            }

            String base = lastSuccess.map(CvsSyncLog::getCommitId).orElse(null);
            GitSyncChangeSet changeSet = changeSetBuilder.build(head, base);
            try {
                synchronizer.sync(changeSet, head);
            } catch (Exception e) {
                failureRecorder.recordFailure(head, errorMessage(e));
            }
        } catch (CvsClientException e) {
            logger.warn("CVS infrastructure failure, will retry: {}", e.getMessage());
        } catch (Exception e) {
            // Configuration errors raised before or while building the change set are
            // recorded as failures so the offending commit is not retried forever.
            if (head == null || head.isBlank()) {
                logger.error("Unexpected failure during configuration synchronization", e);
                return;
            }
            try {
                failureRecorder.recordFailure(head, errorMessage(e));
            } catch (Exception recordError) {
                logger.error("Cannot record failure for commit {}", head, recordError);
            }
        }
    }

    private void initClient() {
        if (!initialized) {
            cvsClient.init();
            initialized = true;
        }
    }

    private String errorMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}