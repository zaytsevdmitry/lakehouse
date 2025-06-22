package org.lakehouse.taskexecutor.executionmodule;
import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractStateTaskProcessor  extends AbstractTaskProcessor {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final StateRestClientApi stateRestClientApi;

	public AbstractStateTaskProcessor(
            TaskProcessorConfig taskProcessorConfig, Jinjava jinjava, StateRestClientApi stateRestClientApi) {
        super(taskProcessorConfig,jinjava);
		this.stateRestClientApi = stateRestClientApi;

    }

	public StateRestClientApi getStateRestClientApi() {
		return stateRestClientApi;
	}

}
