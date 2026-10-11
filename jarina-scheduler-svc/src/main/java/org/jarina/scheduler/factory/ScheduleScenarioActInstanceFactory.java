/*
 * "Jarina dataset self service" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/jarina
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

package org.jarina.scheduler.factory;

import org.jarina.client.api.constant.Status;
import org.jarina.client.api.dto.configs.schedule.ScheduleScenarioActEffectiveDTO;
import org.jarina.scheduler.entities.ScheduleInstance;
import org.jarina.scheduler.entities.ScheduleScenarioActInstance;

public class ScheduleScenarioActInstanceFactory {
    public static ScheduleScenarioActInstance mapToScheduleScenarioActInstance(
            ScheduleScenarioActEffectiveDTO scheduleScenarioActEffectiveDTO,
            ScheduleInstance scheduleInstance) {
        ScheduleScenarioActInstance result = new ScheduleScenarioActInstance();
        result.setName(scheduleScenarioActEffectiveDTO.getName());
        result.setScheduleInstance(scheduleInstance);
        result.setConfDataSetKeyName(scheduleScenarioActEffectiveDTO.getDataSetKeyName());
        result.setStatus(Status.ScenarioAct.NEW);
        return result;
    }
}
