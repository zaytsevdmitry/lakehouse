package org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.spark;

import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.SparkSQLDataSourceManipulatorAbstract;
import org.lakehouse.taskexecutor.api.processor.body.body.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public  class IcebergSparkSQLDataSourceManipulator extends SparkSQLDataSourceManipulatorAbstract {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    public IcebergSparkSQLDataSourceManipulator(
            SparkSQLDataSourceManipulatorParameter sparkSQLDataSourceManipulatorParameter) {
        super(sparkSQLDataSourceManipulatorParameter);
    }


}
