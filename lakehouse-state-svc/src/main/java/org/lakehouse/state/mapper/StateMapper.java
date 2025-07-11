package org.lakehouse.state.mapper;

import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.state.entity.DataSetState;

public  class StateMapper {
     public static DataSetState getState (DataSetStateDTO dataSetStateDTO){
         DataSetState result = new DataSetState();
         result.setDataSetKeyName(dataSetStateDTO.getDataSetKeyName());
         result.setStatus(dataSetStateDTO.getStatus());
         result.setIntervalStartDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(dataSetStateDTO.getIntervalStartDateTime()));
         result.setIntervalEndDateTime(DateTimeUtils.parseDateTimeFormatWithTZ(dataSetStateDTO.getIntervalEndDateTime()));
         result.setLockSource(dataSetStateDTO.getLockSource());
         return result;
    }
    public static DataSetStateDTO getDataSetStateDTO(DataSetState dataSetState){
         DataSetStateDTO result = new DataSetStateDTO();
         result.setDataSetKeyName(dataSetState.getDataSetKeyName());
         result.setStatus(dataSetState.getStatus());
         result.setIntervalStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(dataSetState.getIntervalStartDateTime()));
         result.setIntervalEndDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(dataSetState.getIntervalEndDateTime()));
         result.setLockSource(dataSetState.getLockSource());
         return result;

    }
}
