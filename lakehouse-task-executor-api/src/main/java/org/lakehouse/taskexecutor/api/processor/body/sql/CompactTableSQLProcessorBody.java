package org.lakehouse.taskexecutor.api.processor.body.sql;

import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.taskexecutor.api.processor.body.BodyParam;
import org.springframework.stereotype.Service;

@Service
public class CompactTableSQLProcessorBody extends ScriptSQLProcessorBodyAbstract {
    public CompactTableSQLProcessorBody() {}



    @Override
    public void run(BodyParam bodyParam) throws TaskFailedException {
        execute(
                bodyParam.targetDataSourceManipulator().executeUtils(),
                bodyParam.targetDataSourceManipulator().sqlTemplateDTO().getTableDDLCompact(),
                ""
                );
    }
}
