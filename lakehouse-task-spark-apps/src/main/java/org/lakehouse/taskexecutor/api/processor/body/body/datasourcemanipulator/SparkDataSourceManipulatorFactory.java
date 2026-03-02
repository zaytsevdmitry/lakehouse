package org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.task.SourceConfDTO;
import org.lakehouse.client.api.exception.DDLDIalectException;
import org.lakehouse.client.api.factory.SQLTemplateFactory;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.execute.SparkExecuteUtils;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.execute.SparkExecuteUtilsImpl;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.jdbc.JdbcSparkSQLDataSourceManipulator;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameter;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameterImpl;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.spark.FileSparkSQLDataSourceManipulator;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.spark.IcebergSparkSQLDataSourceManipulator;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
@Service
public class SparkDataSourceManipulatorFactory {
    private final SparkSession sparkSession;
    private final JinJavaUtils jinJavaUtils;
    public SparkDataSourceManipulatorFactory(SparkSession sparkSession, JinJavaUtils jinJavaUtils) {
        this.sparkSession = sparkSession;
        this.jinJavaUtils = jinJavaUtils;
    }

    public DataSourceManipulator buildTargetDataSourceManipulator(SourceConfDTO sourceConfDTO)
            throws UnsuportedDataSourceException, DDLDIalectException, IOException {
        return buildDataSourceManipulator(
                sourceConfDTO.getTargetDriver(),
                sourceConfDTO.getTargetDataSource(),
                sourceConfDTO.getTargetDataSet());
    }

    public DataSourceManipulator buildDataSourceManipulator(
            DriverDTO driverDTO,
            DataSourceDTO dataSourceDTO,
            DataSetDTO dataSetDTO) throws UnsuportedDataSourceException, IOException {
        DataSourceManipulator result = null;
        SparkExecuteUtils executeUtils = new SparkExecuteUtilsImpl(jinJavaUtils, dataSourceDTO, driverDTO, sparkSession);
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
    public Map<String, DataSourceManipulator> buildDataSourceManipulators(
            SourceConfDTO sourceConfDTO)
            throws UnsuportedDataSourceException, DDLDIalectException, IOException {

        Map<String, DataSourceManipulator> result = new HashMap<>();

        for (DataSetDTO dataSetDTO:sourceConfDTO.getDataSets().values()) {
            DataSourceDTO dataSourceDTO = sourceConfDTO.getDataSources().get(dataSetDTO.getDataSourceKeyName());
            DriverDTO driverDTO = sourceConfDTO.getDrivers().get(dataSourceDTO.getDriverKeyName());

            result.put(
                    dataSetDTO.getKeyName(),
                    buildDataSourceManipulator(driverDTO,dataSourceDTO,dataSetDTO));
        }
        return result;
    }
}
