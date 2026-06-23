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

import org.lakehouse.scheduler.entities.ScheduleInstanceLastBuild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ScheduleInstanceLastBuildRepository extends JpaRepository<ScheduleInstanceLastBuild, Long> {

    @Query("""
            select sil\s
            from ScheduleInstanceLastBuild sil\s
             where sil.enabled \s
               and not exists(
                               select sil \
                                 from ScheduleInstanceRunning sir
                                where sir.configScheduleKeyName = sil.configScheduleKeyName\s
            )""")
    List<ScheduleInstanceLastBuild> findByScheduleEnabledNotRunning();


    List<ScheduleInstanceLastBuild> findByEnabled(boolean enabled);

    Optional<ScheduleInstanceLastBuild> findByConfigScheduleKeyName(String configScheduleKeyName);
}
