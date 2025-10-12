package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.JdbcUtils;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.SparkDataStoreManipulator;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.TruncateException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.WriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Map;

public abstract class JdbcSparkDataStoreManipulator extends SparkDataStoreManipulator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final JdbcUtils jdbcUtils;

    public JdbcSparkDataStoreManipulator(
            SparkSession sparkSession,
            String format,
            DataSourceDTO dataSourceDTO,
            JdbcUtils jdbcUtils) {
        super(sparkSession, format, dataSourceDTO);
        this.jdbcUtils = jdbcUtils;
    }

    public JdbcUtils getJdbcUtils() {
        return jdbcUtils;
    }

    @Override
    public void write(
            Dataset<Row> dataset,
            String location,
            Map<String, String> options,
            ModificationRule modificationRule) throws WriteException {
        try {
            dataset.writeTo(location).append();
        } catch (NoSuchTableException e) {
            throw new WriteException(e);
        }
    }

    @Override
    public void truncate(String location, Map<String, String> options) throws TruncateException {
        String truncateSQL = String.format("TRUNCATE TABLE %s", location);
        try {
            jdbcUtils.execute(truncateSQL, options);
        } catch (SQLException e) {
            throw new TruncateException(e);
        }
    }
}
