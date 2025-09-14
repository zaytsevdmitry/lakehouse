package org.lakehouse.taskexecutor.executionmodule.state;

import org.apache.http.HttpStatus;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.executionmodule.AbstractStateTaskProcessor;
import org.lakehouse.taskexecutor.service.DataSetStateDTOFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RunningStateTaskProcessor extends AbstractStateTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public RunningStateTaskProcessor(
            TaskProcessorConfigDTO taskProcessorConfigDTO,
            StateRestClientApi stateRestClientApi) {
        super(taskProcessorConfigDTO, stateRestClientApi);
    }


    @Override
    public void runTask() throws TaskFailedException {
        DataSetStateDTO dataSetStateDTO = DataSetStateDTOFactory.buildtDataSetStateDTO(Status.DataSet.LOCKED,getTaskProcessorConfig());
        logger.info("Send  {}", dataSetStateDTO);
        int resultCode = getStateRestClientApi().setDataSetStateDTO(dataSetStateDTO);
        if( resultCode != HttpStatus.SC_OK ){
            throw new TaskFailedException(String.format("HttpStatus is %d",resultCode));
        }
    }
}
