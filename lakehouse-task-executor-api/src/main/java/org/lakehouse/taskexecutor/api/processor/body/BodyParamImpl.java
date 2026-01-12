package org.lakehouse.taskexecutor.api.processor.body;

import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;

import java.util.Map;
import java.util.Objects;

public final class BodyParamImpl implements BodyParam {
    private final DataSourceManipulator targetDataSourceManipulator;
    private final Map<String, String> taskProcessorArgs;
    private final String fullScript;

    public BodyParamImpl(
            DataSourceManipulator targetDataSourceManipulator,
            Map<String, String> taskProcessorArgs,
            String fullScript) {
        this.targetDataSourceManipulator = targetDataSourceManipulator;
        this.taskProcessorArgs = taskProcessorArgs;
        this.fullScript = fullScript;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        BodyParamImpl bodyParam = (BodyParamImpl) o;
        return Objects.equals(targetDataSourceManipulator, bodyParam.targetDataSourceManipulator) && Objects.equals(taskProcessorArgs, bodyParam.taskProcessorArgs) && Objects.equals(fullScript, bodyParam.fullScript);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetDataSourceManipulator, taskProcessorArgs, fullScript);
    }

    @Override
    public DataSourceManipulator targetDataSourceManipulator() {
        return targetDataSourceManipulator;
    }

    @Override
    public Map<String, String> taskProcessorArgs() {
        return taskProcessorArgs;
    }

    @Override
    public String fullScript() {
        return fullScript;
    }

    @Override
    public String toString() {
        return "BodyParamImpl{" + "targetDataSourceManipulator=" + targetDataSourceManipulator + ", taskProcessorArgs=" + taskProcessorArgs + ", fullScript='" + fullScript + '\'' + '}';
    }

}
