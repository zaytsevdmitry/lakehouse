package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;

import java.util.Map;

public abstract class SparkDataStoreManipulator implements DataStoreManipulator {
    private final SparkSession sparkSession;
    private final String format;
    private final DataSourceDTO dataSourceDTO;

    public SparkDataStoreManipulator(SparkSession sparkSession, String format, DataSourceDTO dataSourceDTO) {
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
}
