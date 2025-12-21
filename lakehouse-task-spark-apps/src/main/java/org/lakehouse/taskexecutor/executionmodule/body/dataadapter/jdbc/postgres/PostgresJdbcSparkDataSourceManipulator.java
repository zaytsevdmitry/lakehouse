package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.postgres;

import org.lakehouse.client.api.constant.Configuration;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulatorParameter;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.*;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.jdbc.JdbcSparkDataSourceManipulatorAbstract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class PostgresJdbcSparkDataSourceManipulator extends JdbcSparkDataSourceManipulatorAbstract {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public PostgresJdbcSparkDataSourceManipulator(
            DataSourceManipulatorParameter dataSourceManipulatorParameter) {
        super(dataSourceManipulatorParameter);
    }

    private void createSchemaIfNotExists() throws CreateException{
        String createCommand = String.format("CREATE SCHEMA IF NOT EXISTS %s", getTableDialect().getDatabaseSchemaName());
        try {
            getJdbcUtils()
                    .execute(
                            createCommand,
                            getJdbcUtils().dtoToProps(getDataSourceDTO())
                    );
        } catch (SQLException e) {
            throw new CreateException("Error when execute " + createCommand,e);
        }
    }
    @Override
    public void createIfNotExists() throws CreateException {

        createSchemaIfNotExists();

        String tableName = getDataSetDTO().getFullTableName();
        String columns = getTableDialect().getColumnsDDL();
        String constraints = ""; // todo
        String partition = ""; // todo
        String createCommand = getTableDialect()
                .getTableDDL()
                .replaceAll("CREATE TABLE","CREATE TABLE IF NOT EXISTS");
        try {
            getJdbcUtils()
                    .execute(
                            createCommand,
                            getJdbcUtils().dtoToProps(getDataSourceDTO())
                    );
        } catch (SQLException e) {
            throw new CreateException("Error when execute " + createCommand,e);
        }
    }

    @Override
    public void drop() throws DropException {
        String createCommand = String.format("DROP TABLE IF EXISTS %s", getDataSetDTO().getFullTableName());
        try {
            getJdbcUtils()
                    .execute(
                            createCommand,
                            getJdbcUtils().dtoToProps(getDataSourceDTO())
                    );
        } catch (SQLException e) {
            throw new DropException("Error when execute " + createCommand,e);
        }
    }

    @Override
    public void dropPartitions(String location, List<String> partitions, Map<String, String> options) throws DropException {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeConstraints() throws ConstraintException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addConstraints() throws ConstraintException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void compact(Map<String, String> options) throws CompactException {
        try {
            getJdbcUtils().execute(
                    String.format("VACUUM %s", getDataSetDTO().getFullTableName()),
                    getJdbcUtils().dtoToProps(getDataSourceDTO()));
        } catch (SQLException e) {
            throw new CompactException(e);
        }
    }

    @Override
    public void compactPartitions(String location, List<String> partitions, Map<String, String> options) throws CompactException {
        throw new UnsupportedOperationException();
    }

}
