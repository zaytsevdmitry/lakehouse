package org.lakehouse.taskexecutor.api.processor.body;

import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.SparkSQLDataSourceManipulator;

import java.util.Map;

public record SparkBodyParamImpl (
            Map<String, SparkSQLDataSourceManipulator> sourceDataSourceManipulatorMap,
            SparkSQLDataSourceManipulator targetDataSourceManipulator,
            Map<String, String> taskProcessorArgs,
            String fullScript) implements SparkBodyParam {}