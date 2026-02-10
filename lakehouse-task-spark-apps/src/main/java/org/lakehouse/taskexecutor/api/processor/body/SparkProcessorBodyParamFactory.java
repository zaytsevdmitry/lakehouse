package org.lakehouse.taskexecutor.api.processor.body;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.lakehouse.client.api.dto.scheduler.lock.ScheduledTaskLockDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.DDLDIalectException;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.executionmodule.body.CatalogActivator;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.SparkDataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.UnsuportedDataSourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SparkProcessorBodyParamFactory {

    private final SparkDataSourceManipulatorFactory sparkDataSourceManipulatorFactory;
    private final ConfigRestClientApi configRestClientApi;
    private final CatalogActivator catalogActivator;
    private final JinJavaUtils jinJavaUtils;
    private final  Logger logger = LoggerFactory.getLogger(SparkProcessorBodyParamFactory.class);

    public SparkProcessorBodyParamFactory(
            SparkDataSourceManipulatorFactory sparkDataSourceManipulatorFactory,
            ConfigRestClientApi configRestClientApi, CatalogActivator catalogActivator, JinJavaUtils jinJavaUtils) {
        this.sparkDataSourceManipulatorFactory = sparkDataSourceManipulatorFactory;
        this.configRestClientApi = configRestClientApi;
        this.catalogActivator = catalogActivator;
        this.jinJavaUtils = jinJavaUtils;
    }


    public  BodyParam buildSparkProcessorBodyParameter(ScheduledTaskLockDTO scheduledTaskLockDTO) throws TaskFailedException {

        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getDataSetKeyName());

        catalogActivator.activate(
                sourceConfDTO.getDataSources().values().stream().toList()
        );


        BodyParam bodyParam = null;
        try {
            jinJavaUtils
                    .injectGlobalContext(ObjectMapping.asMap(sourceConfDTO))
                    .injectGlobalContext(ObjectMapping.asMap(scheduledTaskLockDTO));

            bodyParam = new BodyParamImpl(
                    sparkDataSourceManipulatorFactory.buildTargetDataSourceManipulator(sourceConfDTO),
                    sparkDataSourceManipulatorFactory.buildDataSourceManipulators(sourceConfDTO),
                    scheduledTaskLockDTO.getScheduledTaskEffectiveDTO().getTaskProcessorArgs()

            );
        }catch (UnsuportedDataSourceException | DDLDIalectException | IOException e){
            throw new TaskFailedException(e);
        }
        return bodyParam;
    }
}
