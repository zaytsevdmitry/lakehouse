package org.lakehouse.taskexecutor.api.processor.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.api.datasource.exception.CreateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CreateTableSparkProcessorBody extends SparkProcessorBodyAbstract{
    private final  Logger logger = LoggerFactory.getLogger(this.getClass());

    public CreateTableSparkProcessorBody(SparkSession sparkSession) {
        super(sparkSession);
    }


    @Override
    public void run(BodyParam bodyParam) throws TaskFailedException {

        try {
            bodyParam
                    .targetDataSourceManipulator().createTableIfNotExists();


        } catch (CreateException ue){
            throw  new TaskFailedException(ue);
        }
    }


}
