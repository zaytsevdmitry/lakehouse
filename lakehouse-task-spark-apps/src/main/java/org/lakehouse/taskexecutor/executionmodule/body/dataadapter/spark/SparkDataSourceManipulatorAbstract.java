package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulator;

import java.util.Map;

public abstract class SparkDataSourceManipulatorAbstract implements DataSourceManipulator {
    private final SparkSession sparkSession;
    private final String format;
    private final DataSourceDTO dataSourceDTO;

    public static boolean isCompatible(
            Types.EngineType type,
            Types.Engine serviceType){
        return type.equals(Types.EngineType.spark);
    }
    public SparkDataSourceManipulatorAbstract(SparkSession sparkSession, String format, DataSourceDTO dataSourceDTO) {
        this.sparkSession = sparkSession;
        this.format = format;
        this.dataSourceDTO = dataSourceDTO;
    }

    @Override
    public Dataset<Row> read(String location, Map<String, String> options) {
        return sparkSession.read().format(format).options(options).load();
    }


    public SparkSession getSparkSession() {
        return sparkSession;
    }

    public String getFormat() {
        return format;
    }

    public DataSourceDTO getDataStoreDTO() {
        return dataSourceDTO;
    }

    private Dataset<Row> executeQueryWithPushDown(String query){
        throw new UnsupportedOperationException();
    }
    private Dataset<Row> executeQueryWithOutPushDown(String query){
        return getSparkSession().sql(query);
    }
    @Override
    public Dataset<Row> executeQuery(String query,  boolean enablePushDown) {
        if(enablePushDown){
           return executeQueryWithOutPushDown(query);
        }else{
           return executeQueryWithOutPushDown(query);
        }
    }
}
