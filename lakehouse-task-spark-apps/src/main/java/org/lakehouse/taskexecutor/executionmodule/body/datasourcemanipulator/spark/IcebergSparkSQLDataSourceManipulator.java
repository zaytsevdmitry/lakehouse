package org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.lakehouse.client.api.constant.Configuration;
import org.lakehouse.taskexecutor.api.datasource.exception.ReadException;
import org.lakehouse.taskexecutor.api.datasource.exception.WriteException;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.SparkSQLDataSourceManipulatorAbstract;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public  class IcebergSparkSQLDataSourceManipulator extends SparkSQLDataSourceManipulatorAbstract {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    public IcebergSparkSQLDataSourceManipulator(
            SparkSQLDataSourceManipulatorParameter sparkSQLDataSourceManipulatorParameter) {
        super(sparkSQLDataSourceManipulatorParameter);
    }

    @Override
    public Dataset<Row> read(Map<String,String> options) throws ReadException {
        logger.info("Read dataset from {}.", getCatTableName());
        return sparkSession().table(getCatTableName());
    }

    @Override
    public void write(Dataset<Row> dataset, Configuration.ModificationRule modificationRule) throws WriteException {
        logger.info("Write dataset with {} rows to {}.", dataset.count(),getCatTableName());
        try {
            dataset.writeTo(getCatTableName()).append();
        } catch (NoSuchTableException e) {
            throw new WriteException(e);
        }
    }
}
