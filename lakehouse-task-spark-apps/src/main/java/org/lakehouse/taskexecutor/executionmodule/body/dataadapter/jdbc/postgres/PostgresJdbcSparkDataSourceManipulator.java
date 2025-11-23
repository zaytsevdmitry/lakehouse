package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.postgres;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Configuration;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.JdbcUtils;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.CompactException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.ConstraintException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.DropException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.TruncateException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.JdbcSparkDataSourceManipulatorAbstract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class PostgresJdbcSparkDataSourceManipulator extends JdbcSparkDataSourceManipulatorAbstract {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public PostgresJdbcSparkDataSourceManipulator(
            SparkSession sparkSession,
            DataSourceDTO dataSourceDTO,
            JdbcUtils jdbcUtils) {
        super(sparkSession,  dataSourceDTO, jdbcUtils);
    }

    @Override
    public void drop(String location, Map<String, String> options) throws DropException {

    }

    @Override
    public void dropPartitions(String location, List<String> partitions, Map<String, String> options) throws DropException {

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

    @Override
    public void exchangePartitions(List<String> partitions, String locationFrom, String locationTo, Map<String, String> options, Configuration.ModificationRule modificationRule) {

    }

    @Override
    public void removeConstraints() throws ConstraintException {

    }

    @Override
    public void addConstraints() throws ConstraintException {

    }

    @Override
    public void compact(String location, Map<String, String> options) throws CompactException {
        try {
            getJdbcUtils().execute(String.format("VACUUM %s", location), options);
        } catch (SQLException e) {
            throw new CompactException(e);
        }
    }

    @Override
    public void compactPartitions(String location, List<String> partitions, Map<String, String> options) throws CompactException {

    }

}
