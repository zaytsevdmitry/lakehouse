package org.lakehouse.taskexecutor.processor.jdbc;

import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.task.TaskProcessor;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.execute.ExecuteUtils;
import org.lakehouse.taskexecutor.api.processor.body.BodyParam;
import org.lakehouse.taskexecutor.api.processor.body.BodyParamImpl;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBodyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public  class JdbcTaskProcessor implements TaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DataSourceManipulator dataSourceManipulator;
    private final String fullScript;
    private final Map<String,String> taskProcessorArgs;
    private final String taskProcessorBody;
    public JdbcTaskProcessor(
            DataSourceManipulator dataSourceManipulator,
            String taskProcessorBody,
            Map<String,String> taskProcessorArgs,
            String fullScript) {
        this.dataSourceManipulator = dataSourceManipulator;
        this.taskProcessorBody = taskProcessorBody;
        this.fullScript = fullScript;
        this.taskProcessorArgs = taskProcessorArgs;
    }


    public String getFullScript() {
        return fullScript;
    }

    public SQLTemplateDTO getSqlTemplateDTO() {
        return dataSourceManipulator.sqlTemplateDTO();
    }

    public ExecuteUtils getExecuteUtils() {
        return dataSourceManipulator.executeUtils();
    }

    public DataSourceManipulator getDataSourceManipulator() {
        return dataSourceManipulator;
    }

    @Override
    public void runTask() throws TaskFailedException, TaskConfigurationException {
        logger.info("Making JDBC command class body instance {}", taskProcessorBody);
        BodyParam bodyParam = new BodyParamImpl(
                dataSourceManipulator, taskProcessorArgs,fullScript
        );
        ProcessorBodyFactory.build(bodyParam, taskProcessorBody).run();

    }
}
