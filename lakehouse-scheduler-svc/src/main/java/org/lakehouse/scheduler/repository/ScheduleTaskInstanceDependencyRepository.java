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

import org.lakehouse.scheduler.entities.ScheduleTaskInstanceDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface ScheduleTaskInstanceDependencyRepository extends JpaRepository<ScheduleTaskInstanceDependency, Long> {


    @Query("""
            select  stid s\s
            	from ScheduleInstanceRunning sir\s
            	join ScheduleInstance si                  on si.id   = sir.scheduleInstance.id\s
            	join ScheduleScenarioActInstance ssai     on si.id   = ssai.scheduleInstance.id\s
            	join ScheduleTaskInstance sti             on ssai.id = sti.scheduleScenarioActInstance.id and sti.status = 'SUCCESS'\s
            	join ScheduleTaskInstanceDependency stid  on sti.id  = stid.depends.id                    and stid.satisfied = false\s""")
    List<ScheduleTaskInstanceDependency> findReadyToSatisfied();
}
