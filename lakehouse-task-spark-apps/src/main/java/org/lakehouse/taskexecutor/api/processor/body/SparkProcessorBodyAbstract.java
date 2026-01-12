package org.lakehouse.taskexecutor.api.processor.body;

import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.SparkSQLDataSourceManipulator;

import java.util.Map;

public abstract class SparkProcessorBodyAbstract implements SparkProcessorBody {
    private final SparkBodyParam bodyParam;

    public SparkProcessorBodyAbstract(SparkBodyParam bodyParam) {
        this.bodyParam = bodyParam;

    }


    @Override
    public SparkBodyParam getBodyParam() {
        return bodyParam;
    }

    @Override
    public SparkSQLDataSourceManipulator targetDataSourceManipulator() {
        return targetDataSourceManipulator();
    }

    @Override
    public Map<String, String> taskProcessorArgs() {
        return taskProcessorArgs();
    }

    @Override
    public String fullScript() {
        return fullScript();
    }

    @Override
    public Map<String, SparkSQLDataSourceManipulator> sourceDataSourceManipulatorMap() {
        return sourceDataSourceManipulatorMap();
    }

}
