package org.lakehouse.taskexecutor.api.processor.body;

import com.hubspot.jinjava.Jinjava;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.dataset.DataSetScriptDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.DDLDIalectException;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.taskexecutor.executionmodule.body.CatalogActivator;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.SparkDataSourceManipulatorFactory;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.UnsuportedDataSourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Comparator;
import java.util.stream.Collectors;

public class SparkProcessorBodyParamFactory {

    private final static Logger logger = LoggerFactory.getLogger(SparkProcessorBodyParamFactory.class);
    public static BodyParam buildSparkProcessorBodyParameter(SparkSession sparkSession,
                                                             TaskProcessorConfigDTO taskProcessorConfigDTO ) throws TaskFailedException {

        CatalogActivator catalogActivator = new CatalogActivator(sparkSession);
        catalogActivator.activate(taskProcessorConfigDTO.getDataSources().values().stream().toList());
        BodyParam bodyParam = null;
        try {
            Jinjava jinjava = JinJavaFactory.getJinjava(taskProcessorConfigDTO);
            SparkDataSourceManipulatorFactory sparkDataSourceManipulatorFactory =
                    new SparkDataSourceManipulatorFactory(sparkSession,jinjava);
            String fullScript = taskProcessorConfigDTO
                    .getTargetDataSet()
                    .getScripts().stream()
                    .sorted(Comparator.comparing(DataSetScriptDTO::getOrder))
                    .map(dataSetScriptDTO -> taskProcessorConfigDTO.getScripts().get(dataSetScriptDTO.getKey()))
                    .collect(Collectors.joining("\n;"));

            bodyParam = new SparkBodyParamImpl(
                    sparkDataSourceManipulatorFactory.buildDataSourceManipulators(taskProcessorConfigDTO),
                    sparkDataSourceManipulatorFactory.buildTargetDataSourceManipulator(taskProcessorConfigDTO),
                    taskProcessorConfigDTO.getTaskProcessorArgs(),
                    fullScript
            );
        }catch (UnsuportedDataSourceException | DDLDIalectException | IOException e){
            throw new TaskFailedException(e);
        }
        return bodyParam;
    }
}
