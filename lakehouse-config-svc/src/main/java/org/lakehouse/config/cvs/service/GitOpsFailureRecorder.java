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

package org.lakehouse.config.cvs.service;

import org.lakehouse.config.cvs.entity.CvsSyncLog;
import org.lakehouse.config.cvs.entity.CvsSyncStatus;
import org.lakehouse.config.cvs.repository.CvsSyncLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Persists the failure of a synchronization cycle in an isolated transaction.
 * <p>
 * The regular synchronization transaction has already marked itself rollback-only when a
 * configuration error is detected, so the failure record cannot be stored inside it.
 * REQUIRES_NEW guarantees the record is committed regardless of the main rollback.
 */
@Service
public class GitOpsFailureRecorder {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final CvsSyncLogRepository cvsSyncLogRepository;

    public GitOpsFailureRecorder(CvsSyncLogRepository cvsSyncLogRepository) {
        this.cvsSyncLogRepository = cvsSyncLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String commitId, String errorMessage) {
        logger.error("Synchronization of commit {} failed: {}", commitId, errorMessage);
        CvsSyncLog log = new CvsSyncLog(commitId, OffsetDateTime.now(), CvsSyncStatus.FAILED, errorMessage);
        cvsSyncLogRepository.save(log);
    }
}