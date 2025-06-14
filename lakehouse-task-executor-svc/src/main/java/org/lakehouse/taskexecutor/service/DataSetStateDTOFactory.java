package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;


public class DataSetStateDTOFactory {
    public static DataSetStateDTO buildtDataSetStateDTO(Status.DataSet status, TaskProcessorConfig taskProcessorConfig) {
        DataSetStateDTO result = new DataSetStateDTO();
        result.setDataSetKeyName(taskProcessorConfig.getTargetDataSet().getKeyName());
        result.setIntervalStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(taskProcessorConfig.getIntervalStartDateTime()));
        result.setIntervalEndDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(taskProcessorConfig.getIntervalEndDateTime()));
        result.setStatus(status.label);

        return result;
    }

}
