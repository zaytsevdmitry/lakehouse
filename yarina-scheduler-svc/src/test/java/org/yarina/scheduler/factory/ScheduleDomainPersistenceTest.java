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
package org.yarina.scheduler.factory;

import org.yarina.client.api.dto.configs.schedule.ScheduleEffectiveDTO;
import org.yarina.client.api.dto.scheduler.ScheduleInstanceDTO;
import org.yarina.client.api.utils.DateTimeUtils;
import org.yarina.scheduler.entities.ScheduleInstance;
import org.yarina.scheduler.entities.ScheduleInstanceLastBuild;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The schedule factories are the seam where the domain from the effective configuration must be
 * propagated onto the persisted {@code schedule_instance} / {@code schedule_instance_last_build}
 * entities. Both the domain-present and domain-absent cases are covered.
 */
class ScheduleDomainPersistenceTest {

    private static ScheduleEffectiveDTO effective(String keyName, String domain) {
        ScheduleEffectiveDTO dto = new ScheduleEffectiveDTO();
        dto.setKeyName(keyName);
        dto.setDomainKeyName(domain);
        dto.setEnabled(true);
        dto.setLastChangeNumber(1L);
        dto.setLastChangedDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(DateTimeUtils.now()));
        dto.setStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(DateTimeUtils.now()));
        dto.setIntervalExpression("0 0 0 * * *");
        return dto;
    }

    @Test
    void lastBuildKeepsDomain() {
        ScheduleInstanceLastBuild result = ScheduleInstanceLastBuildFactory.mapDTOToScheduleInstanceLastBuild(
                new ScheduleInstanceLastBuild(), effective("initial", "analytics"));

        assertThat(result.getDomainKeyName()).isEqualTo("analytics");
    }

    @Test
    void lastBuildKeepsNullDomain() {
        ScheduleInstanceLastBuild result = ScheduleInstanceLastBuildFactory.mapDTOToScheduleInstanceLastBuild(
                new ScheduleInstanceLastBuild(), effective("initial", null));

        assertThat(result.getDomainKeyName()).isNull();
    }

    @Test
    void scheduleInstanceKeepsDomain() {
        ScheduleInstanceLastBuild lastBuild = new ScheduleInstanceLastBuild();
        lastBuild.setConfigScheduleKeyName("initial");

        ScheduleInstance result = ScheduleInstanceFactory.newScheduleInstance(
                lastBuild, effective("initial", "analytics"));

        assertThat(result.getDomainKeyName()).isEqualTo("analytics");
    }

    @Test
    void scheduleInstanceKeepsNullDomain() {
        ScheduleInstanceLastBuild lastBuild = new ScheduleInstanceLastBuild();
        lastBuild.setConfigScheduleKeyName("initial");

        ScheduleInstance result = ScheduleInstanceFactory.newScheduleInstance(
                lastBuild, effective("initial", null));

        assertThat(result.getDomainKeyName()).isNull();
    }

    @Test
    void instanceDtoKeepsDomain() {
        ScheduleInstance instance = new ScheduleInstance();
        instance.setConfigScheduleKeyName("initial");
        instance.setDomainKeyName("analytics");
        instance.setTargetExecutionDateTime(DateTimeUtils.now());

        ScheduleInstanceDTO dto = ScheduleInstanceFactory.mapScheduleInstanceDTO(instance);

        assertThat(dto.getDomainKeyName()).isEqualTo("analytics");
    }
}
