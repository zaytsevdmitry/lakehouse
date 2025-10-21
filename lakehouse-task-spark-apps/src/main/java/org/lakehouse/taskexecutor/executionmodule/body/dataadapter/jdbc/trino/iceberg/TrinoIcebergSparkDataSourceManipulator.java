package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.trino.iceberg;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Configuration;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.JdbcUtils;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.CompactException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.trino.TrinoSparkDataSourceManipulatorAbstract;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class TrinoIcebergSparkDataSourceManipulator extends TrinoSparkDataSourceManipulatorAbstract {

    public TrinoIcebergSparkDataSourceManipulator(
            SparkSession sparkSession,

            DataSourceDTO dataSourceDTO,
            JdbcUtils jdbcUtils) {
        super(sparkSession,  dataSourceDTO, jdbcUtils);
    }


    @Override
    public void exchangePartitions(
            List<String> partitions,
            String locationFrom,
            String locationTo,
            Map<String, String> options,
            Configuration.ModificationRule modificationRule) {
        throw new UnsupportedOperationException("exchangePartitions un supported in Iceberg");
    }

    @Override
    public void compact(String location, Map<String, String> options) throws CompactException {
        //todo file_size_threshold => '100MB' is an option
        String compactSQL = String.format("ALTER TABLE %s EXECUTE optimize(file_size_threshold => '100MB')", location);
        try {
            getJdbcUtils().execute(compactSQL, options);
        } catch (SQLException e) {
            throw new CompactException(e);
        }
    }

    public void compactPartitions(String location, List<String> partitions, Map<String, String> options) throws CompactException {

    }

}
