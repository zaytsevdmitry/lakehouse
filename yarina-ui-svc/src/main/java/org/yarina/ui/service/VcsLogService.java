/*
 * "Yarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/yarina
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
package org.yarina.ui.service;

import org.yarina.client.api.dto.vcs.VcsObjectLogDTO;
import org.yarina.client.api.dto.vcs.VcsSyncLogDTO;
import org.yarina.client.rest.config.ConfigRestClientApi;
import org.yarina.domain.settings.DomainSettings;
import org.yarina.ui.modeller.auth.DomainAccess;
import org.yarina.ui.modeller.auth.UserContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
public class VcsLogService {

    private final ConfigRestClientApi configRestClientApi;
    private final DomainSettings domainSettings;

    public VcsLogService(ConfigRestClientApi configRestClientApi, DomainSettings domainSettings) {
        this.configRestClientApi = configRestClientApi;
        this.domainSettings = domainSettings;
    }

    public List<VcsSyncLogDTO> syncLogs(
            OffsetDateTime from, OffsetDateTime to, String status, String commitId, String domainKeyName,
            Authentication authentication) {
        return filterByAllowed(
                configRestClientApi.getVcsSyncLogDTOList(from, to, status, commitId, domainKeyName),
                allowedDomains(authentication, "GET /api/vcs/logs"));
    }

    public List<VcsObjectLogDTO> objectLogs(String commitId, String domainKeyName, Authentication authentication) {
        return filterObjectByAllowed(
                configRestClientApi.getVcsObjectLogDTOList(commitId, null, null, null, null, null, domainKeyName),
                allowedDomains(authentication, "GET /api/vcs/objects"));
    }

    public List<VcsObjectLogDTO> objectLogs(
            String kind, OffsetDateTime from, OffsetDateTime to, String filePath, String objectName,
            String domainKeyName, Authentication authentication) {
        return filterObjectByAllowed(
                configRestClientApi.getVcsObjectLogDTOList(null, kind, from, to, filePath, objectName, domainKeyName),
                allowedDomains(authentication, "GET /api/vcs/objects"));
    }

    private Set<String> allowedDomains(Authentication authentication, String endpoint) {
        return DomainAccess.allowedDomains(
                UserContext.from(authentication), domainSettings.domainNames(), endpoint);
    }

    private static List<VcsSyncLogDTO> filterByAllowed(List<VcsSyncLogDTO> rows, Set<String> allowed) {
        return rows == null ? List.of() : rows.stream()
                .filter(row -> allowed.contains(normalizeDomain(row.getDomainKeyName())))
                .toList();
    }

    private static List<VcsObjectLogDTO> filterObjectByAllowed(List<VcsObjectLogDTO> rows, Set<String> allowed) {
        return rows == null ? List.of() : rows.stream()
                .filter(row -> allowed.contains(normalizeDomain(row.getDomainKeyName())))
                .toList();
    }

    private static String normalizeDomain(String domain) {
        return domain == null ? null : domain.trim();
    }
}