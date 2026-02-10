package org.lakehouse.taskexecutor.api.processor.body;

import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;

import java.util.Map;
import java.util.Objects;

public final class BodyParamImpl implements BodyParam {
    private final DataSourceManipulator targetDataSourceManipulator;
    private final Map<String, String> taskProcessorArgs;
    private final Map<String, DataSourceManipulator> sourceDataSourceManipulatorMap;
    public BodyParamImpl(
            DataSourceManipulator targetDataSourceManipulator,
            Map<String, DataSourceManipulator> sourceDataSourceManipulatorMap,
            Map<String, String> taskProcessorArgs ) {
        this.targetDataSourceManipulator = targetDataSourceManipulator;
        this.taskProcessorArgs = taskProcessorArgs;

        this.sourceDataSourceManipulatorMap = sourceDataSourceManipulatorMap;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BodyParamImpl bodyParam = (BodyParamImpl) o;
        return Objects.equals(targetDataSourceManipulator, bodyParam.targetDataSourceManipulator) && Objects.equals(taskProcessorArgs, bodyParam.taskProcessorArgs) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetDataSourceManipulator, taskProcessorArgs);
    }

    @Override
    public DataSourceManipulator targetDataSourceManipulator() {
        return targetDataSourceManipulator;
    }

    @Override
    public Map<String, DataSourceManipulator> sourceDataSourceManipulatorMap() {
        return sourceDataSourceManipulatorMap;
    }

    @Override
    public Map<String, String> taskProcessorArgs() {
        return taskProcessorArgs;
    }

    @Override
    public String toString() {
        return "BodyParamImpl{" + "targetDataSourceManipulator=" + targetDataSourceManipulator + ", taskProcessorArgs=" + taskProcessorArgs + '\'' + '}';
    }

}
