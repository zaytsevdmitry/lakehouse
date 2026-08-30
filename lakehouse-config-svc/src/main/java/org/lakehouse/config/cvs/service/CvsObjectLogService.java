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

import org.lakehouse.client.api.dto.configs.CvsObjectLogDTO;
import org.lakehouse.config.cvs.entity.CvsObjectLog;
import org.lakehouse.config.cvs.repository.CvsObjectLogRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Read-only queries over the per-object CVS synchronization log.
 */
@Service
public class CvsObjectLogService {

    private final CvsObjectLogRepository cvsObjectLogRepository;

    public CvsObjectLogService(CvsObjectLogRepository cvsObjectLogRepository) {
        this.cvsObjectLogRepository = cvsObjectLogRepository;
    }

    /**
     * Searches object log entries either by commit id, or within the given datetime
     * interval, narrowing optionally by kind, file path (substring, case-insensitive)
     * and object name (substring, case-insensitive).
     */
    @Transactional(readOnly = true)
    public List<CvsObjectLogDTO> find(
            String commitId,
            String kind,
            OffsetDateTime from,
            OffsetDateTime to,
            String filePath,
            String objectName) {
        Specification<CvsObjectLog> spec;
        if (commitId != null && !commitId.isBlank()) {
            spec = (root, query, cb) -> cb.equal(root.get("commitId"), commitId);
        } else {
            spec = (root, query, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.get("dateTimeRec"), from),
                    cb.lessThanOrEqualTo(root.get("dateTimeRec"), to));
        }
        if (kind != null && !kind.isBlank())
            spec = spec.and((root, query, cb) -> cb.equal(root.get("kind"), kind));
        if (filePath != null && !filePath.isBlank())
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("filePath")), "%" + filePath.toLowerCase() + "%"));
        if (objectName != null && !objectName.isBlank())
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("objectName")), "%" + objectName.toLowerCase() + "%"));
        return cvsObjectLogRepository.findAll(spec).stream().map(this::mapToDTO).toList();
    }

    private CvsObjectLogDTO mapToDTO(CvsObjectLog log) {
        CvsObjectLogDTO dto = new CvsObjectLogDTO();
        dto.setId(log.getId());
        dto.setDateTimeRec(log.getDateTimeRec());
        dto.setObjectName(log.getObjectName());
        dto.setKind(log.getKind());
        dto.setFilePath(log.getFilePath());
        dto.setCommitId(log.getCommitId());
        return dto;
    }
}