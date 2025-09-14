package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;

import java.util.Map;

public abstract class SparkFileDataAdapter extends SparkDataAdapter{


    public SparkFileDataAdapter(SparkSession sparkSession, String format, DataStoreDTO dataStoreDTO) {
        super(sparkSession,format,dataStoreDTO);
    }

}
