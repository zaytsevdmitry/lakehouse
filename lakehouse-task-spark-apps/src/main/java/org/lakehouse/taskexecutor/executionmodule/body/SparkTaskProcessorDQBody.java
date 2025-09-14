package org.lakehouse.taskexecutor.executionmodule.body;


import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.client.api.dto.configs.QualityMetricsConfDTO;
import org.lakehouse.client.api.dto.configs.QualityMetricsConfTestSetDTO;
import org.lakehouse.client.api.utils.ObjectMapping;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SparkTaskProcessorDQBody extends SparkProcessorBodyAbstract{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public SparkTaskProcessorDQBody(BodyParam bodyParam) {
        super(bodyParam);
    }



    private QualityMetricsConfDTO getQualityMetricsConfDTO() throws TaskFailedException{
        try {
            return ObjectMapping.stringToObject(getBodyParam().getOtherArgs()[0],QualityMetricsConfDTO.class);
        } catch (IOException e) {
            throw new TaskFailedException(e);
        }
    }

    private void writeMetric(Dataset<Row> dataSet){
        //???
    }
    private void makeMetricTestSet(QualityMetricsConfTestSetDTO testSetDTO){
        Dataset<Row> dataSet =
                getSparkSession().sql(testSetDTO.getValue());
        dataSet.createOrReplaceGlobalTempView(testSetDTO.getKeyName());
        if(testSetDTO.isSave()){
            dataSet.write();
        }
    }

    private void checkTrasHolds(QualityMetricsConfTestSetDTO testSetDTO){

    }
    @Override
    public void run() throws TaskFailedException {

        QualityMetricsConfDTO confDTO = getQualityMetricsConfDTO();
        confDTO.getQualityMetricsConfTestSets().forEach(this::makeMetricTestSet);

    }
}
