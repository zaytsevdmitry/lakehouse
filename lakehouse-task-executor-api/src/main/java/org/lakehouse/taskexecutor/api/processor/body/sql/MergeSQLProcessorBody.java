package org.lakehouse.taskexecutor.api.processor.body.sql;

import org.lakehouse.taskexecutor.api.processor.body.BodyParam;

public class MergeSQLProcessorBody extends ScriptSQLProcessorBodyAbstract {
    public MergeSQLProcessorBody(BodyParam bodyParam) {
        super(bodyParam);
    }

    @Override
    protected String getModelTemplate() {
        return targetDataSourceManipulator().sqlTemplateDTO().getMergeDML();
    }


}
