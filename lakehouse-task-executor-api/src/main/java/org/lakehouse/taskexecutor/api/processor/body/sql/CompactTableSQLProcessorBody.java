package org.lakehouse.taskexecutor.api.processor.body.sql;

import org.lakehouse.taskexecutor.api.processor.body.BodyParam;

public class CompactTableSQLProcessorBody extends ScriptSQLProcessorBodyAbstract {
    public CompactTableSQLProcessorBody(BodyParam bodyParam) {
        super(bodyParam);
    }

    @Override
    protected String getModelTemplate() {
        return targetDataSourceManipulator().sqlTemplateDTO().getTableDDLCompact();
    }


}
