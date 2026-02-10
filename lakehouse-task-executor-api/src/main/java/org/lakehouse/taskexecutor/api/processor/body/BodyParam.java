package org.lakehouse.taskexecutor.api.processor.body;

import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;

import java.util.Map;

public interface BodyParam {
    DataSourceManipulator targetDataSourceManipulator();
    Map<String, DataSourceManipulator> sourceDataSourceManipulatorMap();
    Map<String, String> taskProcessorArgs();

}
