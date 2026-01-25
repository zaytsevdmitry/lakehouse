package org.lakehouse.taskexecutor.api.processor.body;

import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.api.datasource.exception.CreateException;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.SparkSQLDataSourceManipulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class CreateTableSparkProcessorBody extends SparkProcessorBodyAbstract{
    private final  Logger logger = LoggerFactory.getLogger(this.getClass());

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
