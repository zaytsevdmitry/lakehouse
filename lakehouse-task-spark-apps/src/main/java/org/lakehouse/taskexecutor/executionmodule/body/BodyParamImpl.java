package org.lakehouse.taskexecutor.executionmodule.body;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulator;

import java.util.List;
import java.util.Map;

public class BodyParamImpl implements BodyParam{
    private final SparkSession sparkSession;
    private final Map<String,DataSourceManipulator>  sourceDataSourceManipulatorMap;
    private final DataSourceManipulator targetDataSourceManipulator;
    private final Map<String,String> executionArgs;
    private final List<String> scripts;
    private final Map<String,String> keyBind;

    public BodyParamImpl(
            SparkSession sparkSession,
            Map<String, DataSourceManipulator> sourceDataSourceManipulatorMap,
            DataSourceManipulator targetDataSourceManipulator,
            Map<String, String> executionArgs,
            List<String> scripts,
            Map<String, String> keyBind
    ) {
        this.sparkSession = sparkSession;
        this.sourceDataSourceManipulatorMap = sourceDataSourceManipulatorMap;
        this.targetDataSourceManipulator = targetDataSourceManipulator;
        this.executionArgs = executionArgs;
        this.scripts = scripts;
        this.keyBind = keyBind;
    }

    public SparkSession getSparkSession() {
        return sparkSession;
    }

    public Map<String, DataSourceManipulator> getSourceDataSourceManipulatorMap() {
        return sourceDataSourceManipulatorMap;
    }

    public DataSourceManipulator getTargetDataSourceManipulator() {
        return targetDataSourceManipulator;
    }

    public Map<String, String> getExecutionArgs() {
        return executionArgs;
    }

    public List<String> getScripts() {
        return scripts;
    }

    @Override
    public Map<String, String> getKeyBind() {
        return keyBind;
    }
}
