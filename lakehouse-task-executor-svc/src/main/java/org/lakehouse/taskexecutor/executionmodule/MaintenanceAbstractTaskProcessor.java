package org.lakehouse.taskexecutor.executionmodule;

import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;

import java.util.Map;

public abstract class MaintenanceAbstractTaskProcessor extends AbstractTaskProcessor {
    private final Map<String,String> appProperties;
    public MaintenanceAbstractTaskProcessor(TaskProcessorConfig taskProcessorConfig, Map<String,String> appProperties) {
        super(taskProcessorConfig);
        this.appProperties = appProperties;
    }

    public Map<String, String> getAppProperties() {
        return appProperties;
    }
}
