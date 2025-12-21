package org.lakehouse.taskexecutor.executionmodule.jdbc;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.ColumnDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.client.api.dto.task.TaskProcessorConfigDTO;
import org.lakehouse.client.api.exception.TaskFailedException;
import org.lakehouse.client.api.factory.TableDialectFactory;
import org.lakehouse.client.api.factory.dialect.TableDialect;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.taskexecutor.executionmodule.AbstractDefaultTaskProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.stream.Collectors;

//todo this is Demo
public class JdbcAbstractTaskProcessor extends AbstractDefaultTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TaskProcessorConfigDTO taskProcessorConfigDTO;
    private final Jinjava jinjava = new JinJavaFactory().getJinjava();

    public JdbcAbstractTaskProcessor(
            TaskProcessorConfigDTO taskProcessorConfigDTO) {
        super(taskProcessorConfigDTO);
        this.taskProcessorConfigDTO = taskProcessorConfigDTO;

    }

    private String buildDbUrl(DataSourceDTO dataSourceDTO) throws TaskFailedException {
        String result = null;
        ServiceDTO serviceDTO;
        if (dataSourceDTO.getServices().isEmpty())
            throw new TaskFailedException(String.format("DataSource %s with empty list of services", dataSourceDTO.getKeyName()));
        else
            serviceDTO = dataSourceDTO.getServices().get(0);

        if (!dataSourceDTO.getEngineType().equals(Types.EngineType.database))
            throw new TaskFailedException(String.format("DataSource %s is not database", dataSourceDTO.getKeyName()));
        else if (dataSourceDTO.getEngine().equals(Types.Engine.postgres)) {
            result = String.format("jdbc:postgresql://%s:%s/%s", serviceDTO.getHost(), serviceDTO.getPort(), serviceDTO.getUrn());
        } else if (dataSourceDTO.getEngine().toString().startsWith("trino")) {
            result = String.format("jdbc:trino://%s:%s/%s", serviceDTO.getHost(), serviceDTO.getPort(), serviceDTO.getUrn());
        } else {
            throw new TaskFailedException(String.format("DataSource %s with database %s not supported", dataSourceDTO.getKeyName(), dataSourceDTO.getEngine()));
        }
        return result;
    }



    private void resolveTable(Connection connection) throws SQLException {
        String[] table = taskProcessorConfigDTO.getDataSetDTOs().get(
                taskProcessorConfigDTO.getTargetDataSetKeyName()).getFullTableName().split("\\.");

        String checkSQLSchema = String.format(
                "select exists (select * from pg_catalog.pg_namespace where nspname = '%s')",
                table[0]);

        String checkSQLTable = String.format(
                "select exists (\n" +
                        "\tselect * \n" +
                        "\tfrom pg_catalog.pg_class pc \n" +
                        "\twhere relnamespace = (select oid from pg_catalog.pg_namespace where nspname = '%s')\n" +
                        "\tand relname='%s'\n" +
                        ");",
                table[0],
                table[1]
        );

        logger.info("Check schema");
        if (!isObjectExists(connection, checkSQLSchema))
            createSchema(connection, table[0]);
        logger.info("Check table");
        if (!isObjectExists(connection, checkSQLTable))
            createTable(connection);

    }

    private void createSchema(Connection connection, String schemaName) throws SQLException {
        logger.info("Create schema {}", schemaName);
        executeDDL(connection, String.format("create schema %s", schemaName));
    }

    private void executeDDL(Connection connection, String ddl) throws SQLException {
        logger.info("Execute ddl {}", ddl);
        Statement statement = connection.createStatement();
        statement.execute(ddl);
        statement.close();

    }

    private void createTable(Connection connection) throws SQLException {
        logger.info("Create table");
        StringJoiner columns = new StringJoiner(",");
        DataSetDTO targetDataSetDTO = taskProcessorConfigDTO.getDataSetDTOs().get(taskProcessorConfigDTO.getTargetDataSetKeyName());

        targetDataSetDTO.getColumnSchema().stream().map(columnDTO ->
                String.format("%s %s", columnDTO.getName(), columnDTO.getDataType())).forEach(columns::add);

        String createTable = String.format(
                "create table %s (%s)",
                targetDataSetDTO.getFullTableName(),
                columns
                // todo storage parameters ?
                // todo constraints
        );

        executeDDL(connection, createTable);
    }

    private boolean isObjectExists(Connection connection, String checkSQL) {
        boolean result = false;


        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(checkSQL)) {

            while (rs.next()) {
                result = rs.getBoolean(1);
            }

        } catch (SQLException e) {
            logger.warn(e.fillInStackTrace().toString());
            return false;
        }
        return result;

    }


    private boolean isTableExists(Connection connection) {
        String tableName = taskProcessorConfigDTO.getDataSetDTOs().get(
                taskProcessorConfigDTO.getTargetDataSetKeyName()).getFullTableName();

        logger.info("Check table {} exits ", tableName);
        return isObjectExists(connection, String.format("select * from %s where 1=2", tableName));

    }
}
