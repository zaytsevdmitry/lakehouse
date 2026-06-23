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
