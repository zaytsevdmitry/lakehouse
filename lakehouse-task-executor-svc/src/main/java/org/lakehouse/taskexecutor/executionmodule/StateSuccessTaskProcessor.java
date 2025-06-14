package org.lakehouse.taskexecutor.executionmodule;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.utils.DateTimeUtils;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.springframework.http.HttpStatus;


public class StateSuccessTaskProcessor extends MaintenanceAbstractTaskProcessor{


    public StateSuccessTaskProcessor(
            TaskProcessorConfig taskProcessorConfig,
            StateRestClientApi stateRestClientApi,
            Jinjava jinjava) {
        super(taskProcessorConfig, stateRestClientApi, jinjava);
    }

    @Override
    public Status.Task runTask() {
        DataSetStateDTO dataSetStateDTO = new DataSetStateDTO();
        dataSetStateDTO.setStatus(Status.Task.SUCCESS.label);
        dataSetStateDTO.setDataSetKeyName(dataSetStateDTO.getDataSetKeyName());
        dataSetStateDTO
                .setIntervalStartDateTime(
                        DateTimeUtils
                                .formatDateTimeFormatWithTZ(
                                        getTaskProcessorConfig()
                                                .getIntervalStartDateTime()));
        dataSetStateDTO
                .setIntervalEndDateTime(
                        DateTimeUtils
                                .formatDateTimeFormatWithTZ(
                                        getTaskProcessorConfig()
                                                .getIntervalEndDateTime()));

        int httpResultCode = getStateRestClientApi().setDataSetStateDTO(dataSetStateDTO);
        return httpResultCode == HttpStatus.OK.value()? Status.Task.SUCCESS:Status.Task.FAILED;
    }
}
