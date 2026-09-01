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
package org.lakehouse.ui.service;

import org.lakehouse.client.api.dto.configs.VcsObjectLogDTO;
import org.lakehouse.client.api.dto.configs.VcsSyncLogDTO;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class VcsLogService {

    private final ConfigRestClientApi configRestClientApi;

    public VcsLogService(ConfigRestClientApi configRestClientApi) {
        this.configRestClientApi = configRestClientApi;
    }

    public List<VcsSyncLogDTO> syncLogs(
            OffsetDateTime from, OffsetDateTime to, String status, String commitId) {
        return configRestClientApi.getVcsSyncLogDTOList(from, to, status, commitId);
    }

    public List<VcsObjectLogDTO> objectLogs(String commitId) {
        return configRestClientApi.getVcsObjectLogDTOList(commitId, null, null, null, null, null);
    }

    public List<VcsObjectLogDTO> objectLogs(
            String kind, OffsetDateTime from, OffsetDateTime to, String filePath, String objectName) {
        return configRestClientApi.getVcsObjectLogDTOList(null, kind, from, to, filePath, objectName);
    }
}