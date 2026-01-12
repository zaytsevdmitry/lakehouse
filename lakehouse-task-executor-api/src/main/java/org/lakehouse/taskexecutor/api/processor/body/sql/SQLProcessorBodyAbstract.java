package org.lakehouse.taskexecutor.api.processor.body.sql;

import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.processor.body.BodyParam;
import org.lakehouse.taskexecutor.api.processor.body.ProcessorBody;

import java.util.Map;

public abstract class SQLProcessorBodyAbstract implements ProcessorBody {
    private final BodyParam bodyParam;

    public SQLProcessorBodyAbstract(BodyParam bodyParam) {
        this.bodyParam = bodyParam;
    }

    @Override
    public Map<String, String> taskProcessorArgs() {
        return bodyParam.taskProcessorArgs();
    }

    @Override
    public String fullScript() {
        return bodyParam.fullScript();
    }

    @Override
    public DataSourceManipulator targetDataSourceManipulator() {
        return bodyParam.targetDataSourceManipulator();
    }

}
