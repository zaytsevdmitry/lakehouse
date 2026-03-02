package org.lakehouse.taskexecutor.api.processor.body;

import org.lakehouse.client.api.dto.scheduler.tasks.ScheduledTaskDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.DDLDIalectException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.rest.config.ConfigRestClientApi;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.processor.body.body.CatalogActivator;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.SparkDataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.UnsuportedDataSourceException;
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


    public  BodyParam buildSparkProcessorBodyParameter(ScheduledTaskDTO scheduledTaskDTO) throws TaskFailedException {

        SourceConfDTO sourceConfDTO = configRestClientApi.getSourceConfDTO(scheduledTaskDTO.getDataSetKeyName());

        catalogActivator.activate(
                sourceConfDTO.getDataSources().values().stream().toList()
        );


        BodyParam bodyParam = null;
        try {
            jinJavaUtils
                    .injectGlobalContext(ObjectMapping.asMap(sourceConfDTO))
                    .injectGlobalContext(ObjectMapping.asMap(scheduledTaskDTO));

            bodyParam = new BodyParamImpl(
                    sparkDataSourceManipulatorFactory.buildTargetDataSourceManipulator(sourceConfDTO),
                    sparkDataSourceManipulatorFactory.buildDataSourceManipulators(sourceConfDTO),
                    scheduledTaskDTO.getTaskProcessorArgs()
            );
        }catch (UnsuportedDataSourceException | DDLDIalectException | IOException e){
            throw new TaskFailedException(e);
        }
        return bodyParam;
    }
}
