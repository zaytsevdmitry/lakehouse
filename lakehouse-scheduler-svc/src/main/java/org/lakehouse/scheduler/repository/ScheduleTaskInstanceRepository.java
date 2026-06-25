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

package org.lakehouse.scheduler.repository;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.scheduler.entities.ScheduleTaskInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScheduleTaskInstanceRepository extends JpaRepository<ScheduleTaskInstance, Long> {

    @Query("""
            select  sti\s
            	from ScheduleInstanceRunning sir\s
            	join ScheduleInstanceLastBuild sil on sir.configScheduleKeyName = sil.configScheduleKeyName and sil.enabled\s
              	join ScheduleInstance si               on si.id = sir.scheduleInstance.id              and si.status   = 'RUNNING'\s
            	join ScheduleScenarioActInstance ssai  on ssai.scheduleInstance.id  = si.id            and ssai.status = 'RUNNING'\s
            	join ScheduleTaskInstance sti          on sti.scheduleScenarioActInstance.id = ssai.id and sti.status  = 'NEW'\s
            	where not exists (\s
            		select id\s
            			from ScheduleTaskInstanceDependency stid\s
            			where stid.scheduleTaskInstance.id = sti.id\s
            				and not stid.satisfied\s
            	)
            """)
    List<ScheduleTaskInstance> findReadyToQueue();

    List<ScheduleTaskInstance> findByStatus(Status.Task status);
}
