package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;


public class DataSetStateDTOFactory {
    public static DataSetStateDTO buildtDataSetStateDTO(Status.DataSet status, TaskProcessorConfigDTO taskProcessorConfigDTO) {
        DataSetStateDTO result = new DataSetStateDTO();
        result.setDataSetKeyName(taskProcessorConfigDTO.getTargetDataSet().getKeyName());
        result.setIntervalStartDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(taskProcessorConfigDTO.getIntervalStartDateTime()));
        result.setIntervalEndDateTime(DateTimeUtils.formatDateTimeFormatWithTZ(taskProcessorConfigDTO.getIntervalEndDateTime()));
        result.setStatus(status.label);
        result.setLockSource(taskProcessorConfigDTO.getLockSource());

        return result;
    }

}
