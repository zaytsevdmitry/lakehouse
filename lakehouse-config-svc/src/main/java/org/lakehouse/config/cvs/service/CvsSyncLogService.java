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

import org.lakehouse.client.api.dto.configs.CvsSyncLogDTO;
import org.lakehouse.config.cvs.entity.CvsSyncLog;
import org.lakehouse.config.cvs.entity.CvsSyncStatus;
import org.lakehouse.config.cvs.repository.CvsSyncLogRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Read-only queries over the CVS synchronization log.
 */
@Service
public class CvsSyncLogService {

    private final CvsSyncLogRepository cvsSyncLogRepository;

    public CvsSyncLogService(CvsSyncLogRepository cvsSyncLogRepository) {
        this.cvsSyncLogRepository = cvsSyncLogRepository;
    }

    /**
     * Returns synchronization entries within the given interval, optionally narrowed
     * by status and commit id. Both interval bounds are required.
     */
    @Transactional(readOnly = true)
    public List<CvsSyncLogDTO> find(
            OffsetDateTime from,
            OffsetDateTime to,
            String status,
            String commitId) {
        Specification<CvsSyncLog> spec =
                (root, query, cb) -> cb.and(
                        cb.greaterThanOrEqualTo(root.get("syncDateTime"), from),
                        cb.lessThanOrEqualTo(root.get("syncDateTime"), to));
        if (status != null && !status.isBlank())
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), CvsSyncStatus.valueOf(status)));
        if (commitId != null && !commitId.isBlank())
            spec = spec.and((root, query, cb) -> cb.equal(root.get("commitId"), commitId));
        return cvsSyncLogRepository.findAll(spec).stream().map(this::mapToDTO).toList();
    }

    private CvsSyncLogDTO mapToDTO(CvsSyncLog log) {
        CvsSyncLogDTO dto = new CvsSyncLogDTO();
        dto.setId(log.getId());
        dto.setCommitId(log.getCommitId());
        dto.setSyncDateTime(log.getSyncDateTime());
        dto.setStatus(log.getStatus() == null ? null : log.getStatus().name());
        dto.setErrorMessage(log.getErrorMessage());
        return dto;
    }
}