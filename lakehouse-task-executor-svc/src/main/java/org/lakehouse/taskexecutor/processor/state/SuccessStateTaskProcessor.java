package org.lakehouse.taskexecutor.processor.state;

import org.apache.http.HttpStatus;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.state.DataSetStateDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.service.DataSetStateDTOFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service(value = "successStateTaskProcessor")
public class SuccessStateTaskProcessor extends AbstractStateTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public SuccessStateTaskProcessor(
            StateRestClientApi stateRestClientApi) {
        super(stateRestClientApi);
    }


    @Override
    public void runTask(SourceConfDTO sourceConfDTO,
                        ScheduledTaskDTO scheduledTaskDTO,
                        JinJavaUtils jinJavaUtils) throws TaskFailedException {
        DataSetStateDTO dataSetStateDTO = DataSetStateDTOFactory.buildtDataSetStateDTO(Status.DataSet.SUCCESS, scheduledTaskDTO);
        logger.info("Send SUCCESS {}", dataSetStateDTO);
        int resultCode = getStateRestClientApi().setDataSetStateDTO(dataSetStateDTO);
        if (resultCode != HttpStatus.SC_OK) {
            throw new TaskFailedException(String.format("HttpStatus is %d", resultCode));
        }
    }
}
