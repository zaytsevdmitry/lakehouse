package org.lakehouse.taskexecutor.api.processor.body.sql;

import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.api.datasource.exception.CreateException;
import org.lakehouse.taskexecutor.api.processor.body.BodyParam;

public class CreateTableSQLProcessorBody extends SQLProcessorBodyAbstract{
    public CreateTableSQLProcessorBody(BodyParam bodyParam) {
        super(bodyParam);
    }

    @Override
    public void run() throws TaskFailedException {
        try {
            targetDataSourceManipulator().createTableIfNotExists();
        } catch (CreateException e) {
            throw new TaskFailedException(e);
        }

    }


}
