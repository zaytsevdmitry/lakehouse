package org.lakehouse.taskexecutor.api.processor.body.sql;

import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.api.datasource.exception.CreateException;
import org.lakehouse.taskexecutor.api.processor.body.BodyParam;
import org.springframework.stereotype.Service;

@Service(value = "createTableSQLProcessorBody")
public class CreateTableSQLProcessorBody extends SQLProcessorBodyAbstract{
    public CreateTableSQLProcessorBody() {
    }

    @Override
    public void run(BodyParam bodyParam) throws TaskFailedException {
        try {
            bodyParam.targetDataSourceManipulator().createTableIfNotExists();
        } catch (CreateException e) {
            throw new TaskFailedException(e);
        }

    }


}
