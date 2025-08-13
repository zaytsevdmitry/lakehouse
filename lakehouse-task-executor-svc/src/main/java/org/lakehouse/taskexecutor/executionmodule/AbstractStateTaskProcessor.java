package org.lakehouse.taskexecutor.executionmodule;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.common.api.task.processor.entity.TaskProcessorConfigDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractStateTaskProcessor  extends AbstractTaskProcessor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final StateRestClientApi stateRestClientApi;

	public AbstractStateTaskProcessor(
            TaskProcessorConfigDTO taskProcessorConfigDTO, StateRestClientApi stateRestClientApi) {
        super(taskProcessorConfigDTO);
		this.stateRestClientApi = stateRestClientApi;

    }

	public StateRestClientApi getStateRestClientApi() {
		return stateRestClientApi;
	}

}
