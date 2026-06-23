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

package org.lakehouse.state.repository;

import org.lakehouse.state.entity.DataSetState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;

public interface DataSetStateRepository extends JpaRepository<DataSetState, Long> {
    @Query("select d " +
            "from DataSetState d " +
            "where d.dataSetKeyName = ?1 " +
            "and d.intervalStartDateTime <?3 " +
            "and d.intervalEndDateTime > ?2")
    List<DataSetState> findIntersection(String dataSetKeyName, OffsetDateTime intervalStartDateTime, OffsetDateTime intervalEndDateTime);

    @Query("select d " +
            "from DataSetState d " +
            "where d.dataSetKeyName = ?1 " +
            "and d.intervalStartDateTime <?3 " +
            "and d.intervalEndDateTime > ?2")
    List<DataSetState> findEntry(String dataSetKeyName, OffsetDateTime intervalStartDateTime, OffsetDateTime intervalEndDateTime);

}
