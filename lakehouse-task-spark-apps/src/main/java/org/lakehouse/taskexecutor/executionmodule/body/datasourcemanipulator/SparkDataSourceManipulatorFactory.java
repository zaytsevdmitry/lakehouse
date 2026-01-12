package org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator;

import com.hubspot.jinjava.Jinjava;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.DDLDIalectException;
import org.lakehouse.client.api.factory.SQLTemplateFactory;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.execute.SparkExecuteUtils;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.execute.SparkExecuteUtilsImpl;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.jdbc.JdbcSparkSQLDataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameter;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameterImpl;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.spark.FileSparkSQLDataSourceManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.spark.IcebergSparkSQLDataSourceManipulator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SparkDataSourceManipulatorFactory {
    private final SparkSession sparkSession;
    private final Jinjava jinjava;
    public SparkDataSourceManipulatorFactory(SparkSession sparkSession, Jinjava jinjava) {
        this.sparkSession = sparkSession;
        this.jinjava = jinjava;
    }

    public SparkSQLDataSourceManipulator buildTargetDataSourceManipulator(TaskProcessorConfigDTO taskProcessorConfigDTO)
            throws UnsuportedDataSourceException, DDLDIalectException, IOException {
        DriverDTO driverDTO = taskProcessorConfigDTO.getDrivers().get(
                taskProcessorConfigDTO.getTargetDataSourceDTO().getDriverKeyName());
        DataSourceDTO dataSourceDTO = taskProcessorConfigDTO.getTargetDataSourceDTO();
        DataSetDTO dataSetDTO = taskProcessorConfigDTO.getTargetDataSet();

        return buildDataSourceManipulator(driverDTO,dataSourceDTO,dataSetDTO);
    }

    public SparkSQLDataSourceManipulator buildDataSourceManipulator(
            DriverDTO driverDTO,
            DataSourceDTO dataSourceDTO,
            DataSetDTO dataSetDTO) throws UnsuportedDataSourceException, IOException {
        SparkSQLDataSourceManipulator result = null;
        SparkExecuteUtils executeUtils = new SparkExecuteUtilsImpl(jinjava, dataSourceDTO, driverDTO, sparkSession);
        SQLTemplateDTO sqlTemplateDTO = SQLTemplateFactory.mergeSqlTemplate(driverDTO,dataSourceDTO,dataSetDTO);

        SparkSQLDataSourceManipulatorParameter parameter = null;
        parameter = new SparkSQLDataSourceManipulatorParameterImpl(sparkSession,executeUtils,sqlTemplateDTO,dataSetDTO);


        if (driverDTO.getDataSourceType().equals(Types.DataSourceType.database)){
            sqlTemplateDTO = ObjectMapping.fileToObject(getClass().getClassLoader().getResourceAsStream("spark_jdbc_override_template.json"),
                    SQLTemplateDTO.class);
            parameter = new SparkSQLDataSourceManipulatorParameterImpl(sparkSession,executeUtils,sqlTemplateDTO,dataSetDTO);
            result = new JdbcSparkSQLDataSourceManipulator(parameter);
        } else if (driverDTO.getDataSourceType().equals(Types.DataSourceType.iceberg)) {
            result = new IcebergSparkSQLDataSourceManipulator(parameter);
        } else if (driverDTO.getDataSourceType().equals(Types.DataSourceType.file)) {
                result = new FileSparkSQLDataSourceManipulator(parameter);
        } else {
                throw new UnsuportedDataSourceException(
                        String.format(
                                "Driver %s unsupported. Wrong DataSourceType %s",
                                driverDTO.getKeyName(),
                                driverDTO.getDataSourceType()
                        ));
        }
        return result;
    }
    public Map<String, SparkSQLDataSourceManipulator> buildDataSourceManipulators(
            TaskProcessorConfigDTO taskProcessorConfigDTO)
            throws UnsuportedDataSourceException, DDLDIalectException, IOException {

        Map<String, SparkSQLDataSourceManipulator> result = new HashMap<>();

        for (DataSetDTO dataSetDTO:taskProcessorConfigDTO.getDataSets().values()) {
            DataSourceDTO dataSourceDTO = taskProcessorConfigDTO.getDataSources().get(dataSetDTO.getDataSourceKeyName());
            DriverDTO driverDTO = taskProcessorConfigDTO.getDrivers().get(dataSourceDTO.getDriverKeyName());

            result.put(
                    dataSetDTO.getKeyName(),
                    buildDataSourceManipulator(driverDTO,dataSourceDTO,dataSetDTO));
        }
        return result;
    }
}
