package org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator;

import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public  class IcebergSparkSQLDataSourceManipulator extends SparkSQLDataSourceManipulatorAbstract {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    public IcebergSparkSQLDataSourceManipulator(
            SparkSQLDataSourceManipulatorParameter sparkSQLDataSourceManipulatorParameter) {
        super(sparkSQLDataSourceManipulatorParameter);
    }


}
