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

import org.yarina.client.api.dto.common.IntervalDTO;
import org.yarina.client.api.dto.scheduler.ScheduleInstanceDAGDTO;
import org.yarina.client.api.dto.scheduler.ScheduleInstanceDTO;
import org.yarina.client.api.dto.configs.schedule.ScheduleHeaderDTO;
import org.yarina.client.rest.config.ConfigRestClientApi;
import org.yarina.client.rest.scheduler.SchedulerRestClientApi;
import org.yarina.ui.dto.ScheduleRequestDTO;
import org.yarina.domain.settings.DomainSettings;
import org.yarina.ui.modeller.auth.DomainAccess;
import org.yarina.ui.modeller.auth.UserContext;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class ScheduleService {

    private final SchedulerRestClientApi schedulerRestClientApi;
    private final ConfigRestClientApi configRestClientApi;
    private final DomainSettings domainSettings;

    public ScheduleService(
            SchedulerRestClientApi schedulerRestClientApi,
            ConfigRestClientApi configRestClientApi,
            DomainSettings domainSettings) {
        this.schedulerRestClientApi = schedulerRestClientApi;
        this.configRestClientApi = configRestClientApi;
        this.domainSettings = domainSettings;
    }

    public List<ScheduleInstanceDTO> getSchedules(ScheduleRequestDTO request, Authentication authentication) {
        IntervalDTO interval = new IntervalDTO();
        interval.setIntervalStartDateTime(request.getFromDate());
        interval.setIntervalEndDateTime(request.getToDate());

        Set<String> allowed = DomainAccess.allowedDomains(
                UserContext.from(authentication), domainSettings.domainNames(), "POST /api/schedules");
        Stream<ScheduleInstanceDTO> instances;
        List<String> names = request.getNames();
        if (names == null || names.isEmpty()) {
            instances = schedulerRestClientApi.getAllByInterval(interval).stream();
        } else {
            instances = names.stream()
                    .flatMap(name -> schedulerRestClientApi.getAllByInterval(name, interval).stream());
        }
        return instances
                .filter(instance -> allowed.contains(normalizeDomain(instance.getDomainKeyName())))
                .toList();
    }

    public List<ScheduleHeaderDTO> getScheduleHeaders() {
        return configRestClientApi.getScheduleHeaderDTOList();
    }

    public ScheduleInstanceDAGDTO getScheduleInstanceDAG(Long id) {
        return schedulerRestClientApi.getScheduleInstanceDAGDTOById(id);
    }

    private static String normalizeDomain(String domain) {
        return domain == null ? null : domain.trim();
    }
}
