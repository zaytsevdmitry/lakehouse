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

import org.lakehouse.scheduler.entities.ScheduleScenarioActInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface ScheduleScenarioActInstanceRepository extends JpaRepository<ScheduleScenarioActInstance, Long> {
    @Query("""
            select  ssai\s
                from ScheduleInstanceRunning sir
                join ScheduleInstanceLastBuild sil on sir.configScheduleKeyName = sil.configScheduleKeyName and sil.enabled\s
                join ScheduleInstance si              on si.id = sir.scheduleInstance.id    and si.status = 'RUNNING'\s
                join ScheduleScenarioActInstance ssai on ssai.scheduleInstance.id  = si.id  and ssai.status = 'NEW'\s
                    where not exists (\s
                    		select id\s
                        		from ScheduleScenarioActInstanceDependency ssaid\s
                        		where ssaid.to.id = ssai.id\s
                            		and not ssaid.satisfied\s
                )""")
    List<ScheduleScenarioActInstance> findScenarioActReadyToRun();

    @Query("""
            select  ssai\s
                from ScheduleInstanceRunning sir\s
                join ScheduleInstance si              on si.id = sir.scheduleInstance.id    and si.status = 'RUNNING'\s
                join ScheduleScenarioActInstance ssai on ssai.scheduleInstance.id  = si.id  and ssai.status = 'RUNNING'\s
            	where not exists (select 1\s
                                from ScheduleTaskInstance sti\s
                                where ssai.id = sti.scheduleScenarioActInstance.id\s
                                and sti.status != 'SUCCESS')""")
    List<ScheduleScenarioActInstance> findScenarioActReadyToSuccess();
}
