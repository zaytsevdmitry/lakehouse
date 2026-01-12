package org.lakehouse.taskexecutor.api.processor.body.sql;

import org.lakehouse.taskexecutor.api.processor.body.BodyParam;

public class AppendSQLProcessorBody extends ScriptSQLProcessorBodyAbstract {
    public AppendSQLProcessorBody(BodyParam bodyParam) {
        super(bodyParam);
    }

    @Override
    protected String getModelTemplate() {
        return targetDataSourceManipulator().sqlTemplateDTO().getInsertDML();
    }


}
