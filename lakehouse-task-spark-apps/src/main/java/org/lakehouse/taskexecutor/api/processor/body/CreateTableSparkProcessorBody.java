package org.lakehouse.taskexecutor.api.processor.body;

import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.api.datasource.exception.CreateException;

public class CreateTableSparkProcessorBody extends SparkProcessorBodyAbstract{
    public CreateTableSparkProcessorBody(SparkBodyParam bodyParam) {
        super(bodyParam);
    }


    @Override
    public void run() throws TaskFailedException {

        try {
            getBodyParam()
                    .targetDataSourceManipulator().createTableIfNotExists();


        } catch (CreateException ue){
            throw  new TaskFailedException(ue);
        }
    }


}
