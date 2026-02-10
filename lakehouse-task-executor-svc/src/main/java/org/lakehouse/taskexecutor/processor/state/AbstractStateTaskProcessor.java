package org.lakehouse.taskexecutor.processor.state;

import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.taskexecutor.processor.AbstractTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractStateTaskProcessor extends AbstractTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final StateRestClientApi stateRestClientApi;

    public AbstractStateTaskProcessor(
            StateRestClientApi stateRestClientApi) {
        this.stateRestClientApi = stateRestClientApi;

    }

    public StateRestClientApi getStateRestClientApi() {
        return stateRestClientApi;
    }

}
