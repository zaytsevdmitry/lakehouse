/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
