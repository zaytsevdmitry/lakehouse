package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.factory.dialect.TableDialect;

import java.util.Map;

public class DataSourceManipulatorParameterImpl implements DataSourceManipulatorParameter {
    private final SparkSession sparkSession;
    private final DataSourceDTO dataSourceDTO;
    private final DataSetDTO dataSetDTO;
    private final TableDialect tableDialect;
    private final Map<String,String> keyBind;

    public DataSourceManipulatorParameterImpl(
            SparkSession sparkSession,
            DataSourceDTO dataSourceDTO,
            DataSetDTO dataSetDTO,
            TableDialect tableDialect,
            Map<String, String> keyBind) {
        this.sparkSession = sparkSession;
        this.dataSourceDTO = dataSourceDTO;
        this.dataSetDTO = dataSetDTO;
        this.tableDialect = tableDialect;
        this.keyBind = keyBind;
    }

    public SparkSession getSparkSession() {
        return sparkSession;
    }

    public DataSourceDTO getDataSourceDTO() {
        return dataSourceDTO;
    }

    public DataSetDTO getDataSetDTO() {
        return dataSetDTO;
    }

    public TableDialect getTableDialect() {
        return tableDialect;
    }

    public Map<String, String> getKeyBind() {
        return keyBind;
    }
}
