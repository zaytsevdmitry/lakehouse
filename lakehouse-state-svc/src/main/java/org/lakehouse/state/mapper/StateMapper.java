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

package org.lakehouse.state.mapper;

import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.state.entity.DataSetState;

public class StateMapper {
    public static DataSetState getState(DataSetStateDTO dataSetStateDTO) {
        DataSetState result = new DataSetState();
        result.setDataSetKeyName(dataSetStateDTO.getDataSetKeyName());
        result.setStatus(dataSetStateDTO.getStatus());
        result.setIntervalStartDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(dataSetStateDTO.getIntervalStartDateTime()));
        result.setIntervalEndDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(dataSetStateDTO.getIntervalEndDateTime()));
        result.setLockSource(dataSetStateDTO.getLockSource());
        return result;
    }

    public static DataSetStateDTO getDataSetStateDTO(DataSetState dataSetState) {
        DataSetStateDTO result = new DataSetStateDTO();
        result.setDataSetKeyName(dataSetState.getDataSetKeyName());
        result.setStatus(dataSetState.getStatus());
        result.setIntervalStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(dataSetState.getIntervalStartDateTime()));
        result.setIntervalEndDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(dataSetState.getIntervalEndDateTime()));
        result.setLockSource(dataSetState.getLockSource());
        return result;

    }
}
