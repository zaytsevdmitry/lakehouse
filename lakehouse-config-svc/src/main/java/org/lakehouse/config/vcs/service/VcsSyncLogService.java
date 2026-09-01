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

package org.lakehouse.config.vcs.service;

import org.lakehouse.client.api.dto.configs.VcsSyncLogDTO;
import org.lakehouse.config.vcs.entity.VcsSyncLog;
import org.lakehouse.config.vcs.entity.VcsSyncStatus;
import org.lakehouse.config.vcs.repository.VcsSyncLogRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Read-only queries over the VCS synchronization log.
 */
@Service
public class VcsSyncLogService {

    private final VcsSyncLogRepository vcsSyncLogRepository;

    public VcsSyncLogService(VcsSyncLogRepository vcsSyncLogRepository) {
        this.vcsSyncLogRepository = vcsSyncLogRepository;
    }

    /**
     * Returns synchronization entries within the given interval, optionally narrowed
     * by status and commit id. Both interval bounds are required.
     */
    @Transactional(readOnly = true)
    public List<VcsSyncLogDTO> find(
            OffsetDateTime from,
            OffsetDateTime to,
            String status,
            String commitId) {
        Specification<VcsSyncLog> spec =
                (root, query, cb) -> cb.and(
                        cb.greaterThanOrEqualTo(root.get("syncDateTime"), from),
                        cb.lessThanOrEqualTo(root.get("syncDateTime"), to));
        if (status != null && !status.isBlank())
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), VcsSyncStatus.valueOf(status)));
        if (commitId != null && !commitId.isBlank())
            spec = spec.and((root, query, cb) -> cb.equal(root.get("commitId"), commitId));
        return vcsSyncLogRepository.findAll(spec).stream().map(this::mapToDTO).toList();
    }

    private VcsSyncLogDTO mapToDTO(VcsSyncLog log) {
        VcsSyncLogDTO dto = new VcsSyncLogDTO();
        dto.setId(log.getId());
        dto.setCommitId(log.getCommitId());
        dto.setSyncDateTime(log.getSyncDateTime());
        dto.setStatus(log.getStatus() == null ? null : log.getStatus().name());
        dto.setErrorMessage(log.getErrorMessage());
        return dto;
    }
}