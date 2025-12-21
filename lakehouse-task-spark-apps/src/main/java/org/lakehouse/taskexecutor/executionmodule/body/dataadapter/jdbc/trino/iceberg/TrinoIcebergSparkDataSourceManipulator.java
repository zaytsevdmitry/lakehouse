package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.trino.iceberg;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.Configuration;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulatorParameter;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.JdbcUtils;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.CompactException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.DropException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.trino.TrinoSparkDataSourceManipulatorAbstract;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class TrinoIcebergSparkDataSourceManipulator extends TrinoSparkDataSourceManipulatorAbstract {

    public TrinoIcebergSparkDataSourceManipulator(
            DataSourceManipulatorParameter dataSourceManipulatorParameter) {
        super(dataSourceManipulatorParameter);
    }


    @Override
    public void createIfNotExists() {

    }

    @Override
    public void drop() throws DropException {

    }

    @Override
    public void dropPartitions(String location, List<String> partitions, Map<String, String> options) throws DropException {

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
    public void compact(Map<String, String> options) throws CompactException {
        //todo file_size_threshold => '100MB' is an option
        String compactSQL = String.format("ALTER TABLE %s EXECUTE optimize(file_size_threshold => '100MB')", getDataSetDTO().getFullTableName());
        try {
            getJdbcUtils().execute(compactSQL, options);
        } catch (SQLException e) {
            throw new CompactException(e);
        }
    }

    public void compactPartitions(String location, List<String> partitions, Map<String, String> options) throws CompactException {

    }

}
