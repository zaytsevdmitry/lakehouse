package org.lakehouse.taskexecutor.api.processor.body;

import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.SparkSQLDataSourceManipulator;

import java.util.Map;

public interface SparkBodyParam extends BodyParam {
    @Override
    SparkSQLDataSourceManipulator targetDataSourceManipulator();
    Map<String, SparkSQLDataSourceManipulator> sourceDataSourceManipulatorMap();
}
