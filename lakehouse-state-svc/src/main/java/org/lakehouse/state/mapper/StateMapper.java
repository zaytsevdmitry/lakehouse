package org.lakehouse.state.mapper;

import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.state.entity.DataSetState;

public  class StateMapper {
     public static DataSetState getState (DataSetStateDTO dataSetStateDTO){
         DataSetState dataSetState = new DataSetState();
         dataSetState.setDataSetKeyName(dataSetStateDTO.getDataSetKeyName());
         dataSetState.setStatus(dataSetStateDTO.getStatus());
         dataSetState.setIntervalStartDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(dataSetStateDTO.getIntervalStartDateTime()));
         dataSetState.setIntervalEndDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(dataSetStateDTO.getIntervalEndDateTime()));
         return dataSetState;
    }
    public static DataSetStateDTO getDataSetStateDTO(DataSetState dataSetState){
         DataSetStateDTO result = new DataSetStateDTO();
         result.setDataSetKeyName(dataSetState.getDataSetKeyName());
         result.setStatus(dataSetState.getStatus());
         result.setIntervalStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(dataSetState.getIntervalStartDateTime()));
         result.setIntervalEndDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(dataSetState.getIntervalEndDateTime()));
         return result;

    }
}
