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

import org.lakehouse.scheduler.entities.ScheduleInstance;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Transactional
public interface ScheduleInstanceRepository extends JpaRepository<ScheduleInstance, Long> {
    @Query("select p from ScheduleInstance p where p.configScheduleKeyName = ?1")
    List<ScheduleInstance> findByScheduleName(String scheduleName);

    @Query("""
            select p\s
            from ScheduleInstance p\s
            where p.configScheduleKeyName = ?1\s
            and p.targetExecutionDateTime = ?2
            """)
    Optional<ScheduleInstance> findByScheduleNameAndTargetDateTime(String scheduleName,
                                                                   OffsetDateTime targetExecutionDateTime);

    @Query("""
            select si\s
            from ScheduleInstance si\s
            where si.configScheduleKeyName = ?1
            order by si.targetExecutionDateTime desc""")
    List<ScheduleInstance> findByScheduleNameOrderByTargetExecutionDateTimeDesc(String scheduleName, Limit limit);

    @Query("""
            select si\s
            from ScheduleInstance si\s
            where si.configScheduleKeyName = ?1
            and si.targetExecutionDateTime < ?2
            order by si.targetExecutionDateTime desc""")
    List<ScheduleInstance> findByScheduleNameOrderByTargetExecutionDateTimeDescLess(String scheduleName, OffsetDateTime targetExecutionDateTime, Limit limit);

    @Query("""
            select si\s
            from ScheduleInstance si\s
            where si.configScheduleKeyName = ?1
            order by
                  case\s
                   when si.status = 'RUNNING' then 1
                   when si.status = 'NEW'     then 2
                   when si.status = 'SUCCESS' then 3
                  end
                 , si.targetExecutionDateTime
            """)
    List<ScheduleInstance> findByScheduleNameNotSuccessOrderByTargetExecutionDateTimeAsc(String scheduleName,
                                                                                         Limit limit);
}
