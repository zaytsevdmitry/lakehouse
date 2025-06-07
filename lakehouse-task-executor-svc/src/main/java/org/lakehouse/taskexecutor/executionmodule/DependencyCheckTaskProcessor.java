package org.lakehouse.taskexecutor.executionmodule;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.state.DataSetIntervalDTO;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DependencyCheckTaskProcessor extends AbstractTaskProcessor{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final StateRestClientApi stateRestClientApi;
    public DependencyCheckTaskProcessor(
            TaskProcessorConfig taskProcessorConfig,
            StateRestClientApi stateRestClientApi) {
        super(taskProcessorConfig);
        this.stateRestClientApi = stateRestClientApi;
    }

    @Override
    public Status.Task runTask() {
        for (String dataSetKeyName: getTaskProcessorConfig().getDataSetDTOSet().stream().map(DataSetDTO::getKeyName).toList()) {
            DataSetIntervalDTO dataSetIntervalDTO = new DataSetIntervalDTO();
            dataSetIntervalDTO.setDataSetKeyName(dataSetKeyName);
         /*   dataSetIntervalDTO.setIntervalStartDateTime(getTaskProcessorConfig().);
            dataSetIntervalDTO.setIntervalEndDateTime();
         */   stateRestClientApi.getDataSetStateResponseDTO(dataSetIntervalDTO);
        }
        return null;
    }
}
