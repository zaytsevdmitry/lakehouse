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
