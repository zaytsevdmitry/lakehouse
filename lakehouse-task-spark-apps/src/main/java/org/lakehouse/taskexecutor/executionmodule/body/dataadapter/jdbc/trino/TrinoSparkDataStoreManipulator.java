package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.trino;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.JdbcUtils;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.TruncateException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.JdbcSparkDataStoreManipulator;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public abstract class TrinoSparkDataStoreManipulator extends JdbcSparkDataStoreManipulator {

    public TrinoSparkDataStoreManipulator(
            SparkSession sparkSession,
            String format,
            DataSourceDTO dataSourceDTO,
            JdbcUtils jdbcUtils) {
        super(sparkSession, format, dataSourceDTO, jdbcUtils);
    }


    @Override
    public void removeConstraints() {
        throw new UnsupportedOperationException("Constraints un supported in Trino");
    }

    @Override
    public void addConstraints() {
        throw new UnsupportedOperationException("Constraints un supported in Trino");
    }

    @Override
    public void truncatePartitions(
            String location,
            List<String> partitions,
            Map<String, String> options) throws TruncateException {

        for (String partitionValue : partitions) {
            try {
                String truncateSql = String.format("ALTER TABLE %s TRUNCATE PARTITION (partition_column = '%s')", location, partitionValue);
                getJdbcUtils().execute(truncateSql, options);
            } catch (SQLException e) {
                throw new TruncateException(e);
            }
        }

    }

}
