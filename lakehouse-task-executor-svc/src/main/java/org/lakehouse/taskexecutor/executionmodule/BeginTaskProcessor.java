package org.lakehouse.taskexecutor.executionmodule;

import com.hubspot.jinjava.Jinjava;
import org.apache.http.HttpStatus;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.service.DataSetStateDTOFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeginTaskProcessor extends AbstractTaskProcessor{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final StateRestClientApi stateRestClientApi;
    public BeginTaskProcessor(
            TaskProcessorConfig taskProcessorConfig,
            StateRestClientApi stateRestClientApi,
            Jinjava jinjava) {
        super(taskProcessorConfig,jinjava);
        this.stateRestClientApi = stateRestClientApi;
    }

    @Override
    public Status.Task runTask() {
        DataSetStateDTO dataSetStateDTO = DataSetStateDTOFactory.buildtDataSetStateDTO(Status.DataSet.RUNNING,getTaskProcessorConfig());
        logger.info("Send  {}", dataSetStateDTO);
        int resultCode = stateRestClientApi.setDataSetStateDTO(dataSetStateDTO);
        return resultCode == HttpStatus.SC_OK ? Status.Task.SUCCESS: Status.Task.FAILED;
    }
}
