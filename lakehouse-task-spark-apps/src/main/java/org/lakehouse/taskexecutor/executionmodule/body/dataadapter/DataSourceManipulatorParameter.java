package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.factory.dialect.TableDialect;

import java.util.Map;

public interface DataSourceManipulatorParameter {
    public SparkSession getSparkSession();
    public DataSourceDTO getDataSourceDTO();
    public DataSetDTO getDataSetDTO();
    public TableDialect getTableDialect();
    public Map<String, String> getKeyBind();
}
