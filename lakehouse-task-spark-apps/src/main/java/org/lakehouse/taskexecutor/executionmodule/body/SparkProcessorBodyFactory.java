package org.lakehouse.taskexecutor.executionmodule.body;

import com.hubspot.jinjava.Jinjava;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.execution.datasources.DataSource;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.factory.TableDialectFactory;
import org.lakehouse.client.api.factory.dialect.TableDialect;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulatorParameter;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.UnsuportedDataSourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;

public class SparkProcessorBodyFactory {

    private final static Logger logger = LoggerFactory.getLogger(SparkProcessorBodyFactory.class);
    public static SparkProcessorBody buildSparkProcessorBody(
            SparkSession sparkSession,
            String[] args)
            throws TaskFailedException {

        TaskProcessorConfigDTO taskProcessorConfigDTO = null;

        try {
            logger.info("Build TaskProcessorConfigDTO from JSON\n{}",args[0]);
            taskProcessorConfigDTO = ObjectMapping.stringToObject(args[0], TaskProcessorConfigDTO.class);
        } catch (IOException e) {
            throw new TaskFailedException(e);
        }

        CatalogActivator catalogActivator = new CatalogActivator(sparkSession);
        catalogActivator.activate(taskProcessorConfigDTO.getDataSources().values().stream().toList());

        String executionBodyName = taskProcessorConfigDTO.getExecutionModuleArgs().get("executionBody");

        if (executionBodyName == null || executionBodyName.isBlank())
            throw new TaskFailedException("Argument 'executionBody' is empty");

        String[] leastArgs;

        if (args.length > 1) {
            leastArgs = new String[args.length - 1];
            for (int i = 1; i <= args.length; i++)
                leastArgs[i] = args[i];
        } else leastArgs = new String[0];

        DataSourceManipulatorFactory dataSourceManipulatorFactory = new DataSourceManipulatorFactory();


        BodyParam bodyParam = null;

        try {

            bodyParam = new BodyParamImpl(
                    sparkSession,
                    dataSourceManipulatorFactory.buildDataSourceManipulators(sparkSession, taskProcessorConfigDTO),
                    dataSourceManipulatorFactory.buildTargetDataSourceManipulator(sparkSession, taskProcessorConfigDTO),
                    taskProcessorConfigDTO.getExecutionModuleArgs(),
                    taskProcessorConfigDTO.getScripts(),
                    taskProcessorConfigDTO.getKeyBind()
            );

        }catch (UnsuportedDataSourceException e){
            throw new TaskFailedException(e);
        }


        Class<?> processorBodyClass = null;

        try {
            processorBodyClass = Class.forName(executionBodyName);
        } catch (ClassNotFoundException e) {
            throw new TaskFailedException(e.getMessage(), e);
        }

        if (SparkProcessorBodyAbstract.class.isAssignableFrom(processorBodyClass)) {
            Constructor<?> constructor = null;

            try {
                constructor = processorBodyClass.getConstructor(BodyParam.class);
            } catch (NoSuchMethodException e) {
                throw new TaskFailedException(String.format("Wrong constructor of %s", executionBodyName), e);
            }

            SparkProcessorBody result = null;

            try {
                result = (SparkProcessorBody) constructor.newInstance(bodyParam);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                throw new TaskFailedException(String.format("Can't create instance of body %s", executionBodyName), e);
            }
            return result;
        } else {
            throw new TaskFailedException("Its not a SparkProcessorBodyAbstract type");
        }
    }
}
