package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.trino;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.JdbcUtils;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.TruncateException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.JdbcSparkDataSourceManipulatorAbstract;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public abstract class TrinoSparkDataSourceManipulatorAbstract extends JdbcSparkDataSourceManipulatorAbstract {

    public TrinoSparkDataSourceManipulatorAbstract(
            SparkSession sparkSession,
            DataSourceDTO dataSourceDTO,
            JdbcUtils jdbcUtils) {
        super(sparkSession, dataSourceDTO, jdbcUtils);
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
