package org.lakehouse.taskexecutor.executionmodule.body;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulator;
import java.util.List;
import java.util.Map;

interface BodyParam {


    SparkSession getSparkSession();

    Map<String, DataSourceManipulator> getSourceDataSourceManipulatorMap();
    DataSourceManipulator getTargetDataSourceManipulator();
    Map<String, String> getExecutionArgs();
    List<String> getScripts();
    Map<String, String> getKeyBind();


}
