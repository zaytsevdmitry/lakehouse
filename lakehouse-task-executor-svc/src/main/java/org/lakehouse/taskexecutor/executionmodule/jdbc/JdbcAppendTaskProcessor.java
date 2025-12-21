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
public class JdbcAppendTaskProcessor extends JdbcAbstractTaskProcessor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TaskProcessorConfigDTO taskProcessorConfigDTO;
    private final Jinjava jinjava = new JinJavaFactory().getJinjava();

    public JdbcAppendTaskProcessor(
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

    @Override
    public void runTask() throws TaskFailedException {

        /* Map<String, String> keyBind = new HashMap<>();
        keyBind.putAll(taskProcessorConfig.getKeyBind());
        keyBind.putAll(
                taskProcessorConfig.getDataSetDTOSet().stream()
                        .collect(Collectors.toMap(dataSetDTO ->
                                        String.format("${source(%s)}", dataSetDTO.getKeyName()),
                                DataSetDTO::getFullTableName))
        );
        taskProcessorConfig.setKeyBind(keyBind);
*/
        DataSetDTO targetDataSetDTO = taskProcessorConfigDTO.getTargetDataSet();

        DataSourceDTO targetDataSourceDTO = taskProcessorConfigDTO.getTargetDataSourceDTO();

        try {
            logger.info("JdbcTaskProcessor.run >> dataSet={}",
                    taskProcessorConfigDTO.getTargetDataSetKeyName());

            TableDialect tableDialect = new TableDialectFactory().buildTableDialect(
                    targetDataSourceDTO,
                    targetDataSetDTO,
                    taskProcessorConfigDTO.getForeignDataSetDTOMap()
            );

            Properties properties = new Properties();
            properties.putAll(targetDataSourceDTO.getProperties());
            properties.putAll(targetDataSourceDTO.getServices().get(0).getProperties());

            String url = buildDbUrl(targetDataSourceDTO);


            logger.info("Connecting to database {}...", url);
            try (
                    Connection connection = DriverManager.getConnection(url, properties);
                    Statement statement = connection.createStatement()) {

                logger.info("Take script");
                /*//todo MVP take only one script
                String unfeeledSQL = taskProcessorConfig.getScripts().get(0);
*/
                logger.info("resolve table changes");
                resolveTable(connection);


                logger.info("Feel script");


                String columns =
                        targetDataSetDTO
                                .getColumnSchema()
                                .stream()
                                .filter(columnDTO -> !columnDTO.getDataType().equalsIgnoreCase("serial"))
                                .map(ColumnDTO::getName)
                                .collect(Collectors.joining(", "));

                String sql = jinjava.render(
                        String.format(
                                "insert into %s (%s)\n select %s\n from (\n%s\n)",
                                targetDataSetDTO.getFullTableName(),
                                tableDialect.getColumnsComaSeparated(),
                                tableDialect.getColumnsComaSeparated(),
                                taskProcessorConfigDTO.getScripts().get(0)
                        ),
                        taskProcessorConfigDTO.getKeyBind());

                logger.info("Creating statement...{}", sql);


                Boolean isRetrieved = statement.execute(sql);
                logger.info("Script execution is done");


            } catch (SQLException e) {
                logger.error(e.getMessage());
                throw new TaskFailedException(e);
            }

        } catch (Exception e) {
            logger.error("Error task execution", e);
            throw new TaskFailedException(e);
        }
    }

/*
    public String feelScripts(String script) {
        String result = script;

        for (Map.Entry<String, String> sse : taskProcessorConfig
                .getKeyBind()
                .entrySet()) {
            logger.info("Replace {} to {}", sse.getKey(), sse.getValue());
            result = result.replace(sse.getKey(), sse.getValue());
        }
        logger.info("Prepared query looks as {}", result);
        return result;
    }
*/

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
