package org.lakehouse.taskexecutor.service;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;



public class DataSetStateDTOFactory {
    public static DataSetStateDTO buildtDataSetStateDTO(
            Status.DataSet status, ScheduledTaskDTO scheduledTaskDTO) {
        DataSetStateDTO result = new DataSetStateDTO();
        result.setDataSetKeyName(scheduledTaskDTO.getDataSetKeyName());
        result.setIntervalStartDateTime(scheduledTaskDTO.getIntervalStartDateTime());
        result.setIntervalEndDateTime(scheduledTaskDTO.getIntervalEndDateTime());
        result.setStatus(status);
        result.setLockSource(scheduledTaskDTO.getLockSource());

        return result;
    }

}
