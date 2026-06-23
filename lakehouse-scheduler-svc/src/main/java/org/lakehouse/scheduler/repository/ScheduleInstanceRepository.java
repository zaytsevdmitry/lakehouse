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
