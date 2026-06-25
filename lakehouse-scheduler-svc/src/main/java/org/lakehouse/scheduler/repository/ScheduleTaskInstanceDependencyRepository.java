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
