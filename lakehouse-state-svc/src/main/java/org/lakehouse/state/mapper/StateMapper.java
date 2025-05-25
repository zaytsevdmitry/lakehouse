package org.lakehouse.state.mapper;

import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.state.entity.DataSetState;

public  class StateMapper {
     public static DataSetState getState (DataSetStateDTO dataSetStateDTO){
         DataSetState dataSetState = new DataSetState();
         dataSetState.setDataSetKeyName(dataSetStateDTO.getDataSetKeyName());
         dataSetState.setStatus(dataSetStateDTO.getStatus());
         dataSetState.setIntervalStartDateTime(DateTimeUtils.parceDateTimeFormatWithTZ(dataSetStateDTO.getIntervalStartDateTime()));
         dataSetState.setIntervalEndDateTime(DateTimeUtils.parceDateTimeFormatWithTZ(dataSetStateDTO.getIntervalEndDateTime()));
         return dataSetState;
    }
}
