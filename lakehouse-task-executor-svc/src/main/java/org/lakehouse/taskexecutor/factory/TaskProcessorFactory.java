package org.lakehouse.taskexecutor.factory;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.dto.task.TaskProcessor;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.rest.state.StateRestClientApi;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.processor.AbstractDefaultTaskProcessor;
import org.lakehouse.taskexecutor.processor.AbstractSparkDeployTaskProcessor;
import org.lakehouse.taskexecutor.processor.AbstractStateTaskProcessor;
import org.lakehouse.taskexecutor.processor.jdbc.JdbcTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;


@Service
public class TaskProcessorFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final StateRestClientApi stateRestClientApi;
    public TaskProcessorFactory(
            StateRestClientApi stateRestClientApi) {
        this.stateRestClientApi = stateRestClientApi;
    }
    private DataSourceManipulator getDataSourceManipulator(
            TaskProcessorConfigDTO  taskProcessorConfigDTO,
            Jinjava jinjava)
            throws IOException{

        return DataSourceManipulatorFactory
                .buildDataSourceManipulator(taskProcessorConfigDTO, jinjava);

    }
    private TaskProcessor constructTaskProcessor(
            Class<?> processorClass,
            TaskProcessorConfigDTO taskProcessorConfigDTO)
            throws TaskConfigurationException {

        TaskProcessor result = null;

        try {

            Jinjava jinjava = JinJavaFactory.getJinjava(taskProcessorConfigDTO);
            Constructor<?> constructor = null;

            if (JdbcTaskProcessor.class.isAssignableFrom(processorClass)){
                logger.info("Making JDBC command class instance {}", processorClass.getName());
                constructor = processorClass.getConstructor(DataSourceManipulator.class, String.class,Map.class, String.class);
                result =  (TaskProcessor) constructor.newInstance(
                        getDataSourceManipulator(taskProcessorConfigDTO,jinjava),
                        taskProcessorConfigDTO.getTaskProcessorBody(),
                        taskProcessorConfigDTO.getTaskProcessorArgs(),
                        taskProcessorConfigDTO.getTargetFullScript());
            }else if (AbstractStateTaskProcessor.class.isAssignableFrom(processorClass)) {
                logger.info("Making State maintenance class instance {}", processorClass.getName());
                constructor = processorClass.getConstructor(TaskProcessorConfigDTO.class, StateRestClientApi.class);
                result = (TaskProcessor) constructor.newInstance(taskProcessorConfigDTO, stateRestClientApi);
            } else if (AbstractDefaultTaskProcessor.class.isAssignableFrom(processorClass)) {
                logger.info("Making Default processor class instance {}", processorClass.getName());
                constructor = processorClass.getConstructor(TaskProcessorConfigDTO.class);
                result = (TaskProcessor) constructor.newInstance(taskProcessorConfigDTO);
            } else if (AbstractSparkDeployTaskProcessor.class.isAssignableFrom(processorClass)) {
                logger.info("Making Spark deployment processor class instance {}", processorClass.getName());
                constructor = processorClass.getConstructor(TaskProcessorConfigDTO.class);
                result = (TaskProcessor) constructor.newInstance(taskProcessorConfigDTO);
            } else {
                throw new TaskConfigurationException(
                        String.format("Processor class found, but has unexpected type : class name  %s", processorClass.getName()));
            }

        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | IOException |
                 InvocationTargetException e) {
            throw new TaskConfigurationException(String.format("Class '%s' constructor error", processorClass.getName()), e);
        }
        return result;
    }

    public TaskProcessor buildProcessor(TaskProcessorConfigDTO taskProcessorConfigDTO, String executionModule)
            throws TaskConfigurationException {

        Class<?> processorClass = null;
        logger.info("Try load class:{}", executionModule);
        try {
            processorClass = Class.forName(executionModule);
        } catch (ClassNotFoundException e) {
            throw new TaskConfigurationException("Class instantiate error", e);
        }

        logger.info("Loaded class:{}", processorClass.getName());
        logger.info("Define constructor");
        return constructTaskProcessor(processorClass, taskProcessorConfigDTO);
    }

}
