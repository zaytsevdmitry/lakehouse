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
package org.lakehouse.ui.controller;

import org.lakehouse.client.api.dto.configs.VcsObjectLogDTO;
import org.lakehouse.client.api.dto.configs.VcsSyncLogDTO;
import org.lakehouse.ui.service.VcsLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/vcs")
public class VcsLogController {

    private final VcsLogService vcsLogService;

    public VcsLogController(VcsLogService vcsLogService) {
        this.vcsLogService = vcsLogService;
    }

    @GetMapping("/logs")
    public List<VcsSyncLogDTO> syncLogs(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String commitId) {
        return vcsLogService.syncLogs(from, to, status, commitId);
    }

    @GetMapping("/objects")
    public List<VcsObjectLogDTO> objects(
            @RequestParam(required = false) String commitId,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String filePath,
            @RequestParam(required = false) String objectName) {
        if (commitId != null && !commitId.isBlank()) {
            return vcsLogService.objectLogs(commitId);
        }
        return vcsLogService.objectLogs(kind, from, to, filePath, objectName);
    }
}