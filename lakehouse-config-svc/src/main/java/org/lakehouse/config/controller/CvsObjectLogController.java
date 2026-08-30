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

import io.swagger.v3.oas.annotations.tags.Tag;
import org.lakehouse.client.api.constant.Endpoint;
import org.lakehouse.client.api.dto.configs.CvsObjectLogDTO;
import org.lakehouse.config.cvs.service.CvsObjectLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Read-only access to the per-object CVS synchronization log.
 */
@RestController
@Tag(name = "CVS object log")
public class CvsObjectLogController {

    private final CvsObjectLogService cvsObjectLogService;

    public CvsObjectLogController(CvsObjectLogService cvsObjectLogService) {
        this.cvsObjectLogService = cvsObjectLogService;
    }

    /**
     * Returns object log entries. Either {@code commitId} or both {@code from} and
     * {@code to} must be supplied; kind, filePath and objectName are optional filters.
     */
    @GetMapping(Endpoint.CVS_OBJECT_LOGS)
    List<CvsObjectLogDTO> find(
            @RequestParam(required = false) String commitId,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String filePath,
            @RequestParam(required = false) String objectName) {
        validateEitherCommitOrInterval(commitId, from, to);
        return cvsObjectLogService.find(commitId, kind, from, to, filePath, objectName);
    }

    private void validateEitherCommitOrInterval(String commitId, OffsetDateTime from, OffsetDateTime to) {
        boolean hasCommit = commitId != null && !commitId.isBlank();
        boolean hasInterval = from != null && to != null;
        if (!hasCommit && !hasInterval)
            throw new IllegalArgumentException(
                    "Either commitId or both from and to must be provided");
    }
}