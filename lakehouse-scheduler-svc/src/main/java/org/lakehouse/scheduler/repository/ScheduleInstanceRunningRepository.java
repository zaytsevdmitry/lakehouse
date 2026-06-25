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

import org.lakehouse.scheduler.entities.ScheduleInstanceRunning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ScheduleInstanceRunningRepository extends JpaRepository<ScheduleInstanceRunning, Long> {
    @Query("select p from ScheduleInstanceRunning p where p.configScheduleKeyName = ?1")
    List<ScheduleInstanceRunning> findByScheduleName(String scheduleName);

    @Query("""
            select sir\s
            from ScheduleInstanceRunning sir\s
            join ScheduleInstanceLastBuild sil on sir.configScheduleKeyName = sil.configScheduleKeyName and sil.enabled\s
            where sir.scheduleInstance is null""")
    List<ScheduleInstanceRunning> findScheduleInstanceNull();

    @Query("""
            select sir\s
            from ScheduleInstanceRunning sir\s
            join ScheduleInstanceLastBuild sil on sir.configScheduleKeyName = sil.configScheduleKeyName and sil.enabled
            where sir.scheduleInstance.status  in ('SUCCESS', 'FAILED')\s""")
    List<ScheduleInstanceRunning> findByScheduleEnabledAndStatusSuccessAndStatusFAiled();

    @Query("""
            select  sir\s
                from ScheduleInstanceRunning sir
                join ScheduleInstanceLastBuild sil on sir.configScheduleKeyName = sil.configScheduleKeyName and sil.enabled
                join ScheduleInstance si on si.id = sir.scheduleInstance.id  and si.status = 'RUNNING'
            where not exists (select 1
                                from ScheduleScenarioActInstance ssai\s
                                where  ssai.scheduleInstance.id  = si.id\s
                                and ssai.status != 'SUCCESS')
            """)
    List<ScheduleInstanceRunning> findByScheduleReadyToSuccess();
}
