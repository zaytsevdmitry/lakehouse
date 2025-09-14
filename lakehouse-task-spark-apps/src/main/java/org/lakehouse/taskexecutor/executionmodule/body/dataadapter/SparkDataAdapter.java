package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;

import java.util.Map;

public abstract class SparkDataAdapter implements DataAdapter{
    private final SparkSession sparkSession;
    private final String format;
    private final DataStoreDTO dataStoreDTO;

    public SparkDataAdapter(SparkSession sparkSession, String format, DataStoreDTO dataStoreDTO) {
        this.sparkSession = sparkSession;
        this.format = format;
        this.dataStoreDTO = dataStoreDTO;
    }

    @Override
    public Dataset<Row> read(String location, Map<String, String> options) {
        return sparkSession.read().format(format).options(options).load();

    }

    @Override
    public void write(Dataset<Row> dataset, String location, Map<String, String> options) {

    }

    @Override
    public void clear(String location) {

    }

    public SparkSession getSparkSession() {
        return sparkSession;
    }

    public String getFormat() {
        return format;
    }

    public DataStoreDTO getDataStoreDTO() {
        return dataStoreDTO;
    }
}
