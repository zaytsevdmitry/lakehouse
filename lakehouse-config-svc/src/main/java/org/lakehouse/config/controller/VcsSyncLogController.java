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
import org.lakehouse.client.api.dto.configs.VcsSyncLogDTO;
import org.lakehouse.config.vcs.service.VcsSyncLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Read-only access to the VCS synchronization log.
 */
@RestController
@Tag(name = "VCS sync log")
public class VcsSyncLogController {

    private final VcsSyncLogService vcsSyncLogService;

    public VcsSyncLogController(VcsSyncLogService vcsSyncLogService) {
        this.vcsSyncLogService = vcsSyncLogService;
    }

    @GetMapping(Endpoint.VCS_SYNC_LOGS)
    List<VcsSyncLogDTO> find(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String commitId) {
        return vcsSyncLogService.find(from, to, status, commitId);
    }
}