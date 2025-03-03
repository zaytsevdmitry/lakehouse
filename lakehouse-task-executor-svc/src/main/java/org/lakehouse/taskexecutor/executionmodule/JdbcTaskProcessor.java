package org.lakehouse.taskexecutor.executionmodule;

import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.ColumnDTO;
import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.lakehouse.taskexecutor.service.TableDefinitionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;


public  class JdbcTaskProcessor extends AbstractTaskProcessor{
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final TaskProcessorConfig taskProcessorConfig;
	public JdbcTaskProcessor(
			TaskProcessorConfig taskProcessorConfig) {
		super(taskProcessorConfig);
		this.taskProcessorConfig = taskProcessorConfig;
	}


	@Override
	public Status.Task runTask() {
		Map<String,String> keyBind = new HashMap<>();
		keyBind.putAll(taskProcessorConfig.getKeyBind());
		keyBind.putAll(
				taskProcessorConfig.getDataSetDTOSet().stream()
				.collect(Collectors.toMap(dataSetDTO -> 
						String.format("${source(%s)}", dataSetDTO.getName()),
                        DataSetDTO::getFullTableName))
		);
		taskProcessorConfig.setKeyBind(keyBind);
		
	try{
		logger.info("JdbcTaskProcessor.run >> dataSet={}, dataStore={}",
				taskProcessorConfig.getTargetDataSet().getName(),
				taskProcessorConfig.getTargetDataSet().getDataStore()
				)
		;
		DataStoreDTO ds = taskProcessorConfig
				.getDataStores()
				.get(taskProcessorConfig
						.getTargetDataSet()
						.getDataStore());

		Properties properties = new Properties();
	    properties.putAll(ds.getProperties());
		logger.info("Registering JDBC driver...key driver");
		logger.info("Connecting to database...");
		try(
				Connection connection = DriverManager.getConnection(ds.getUrl(),properties);
				Statement statement = connection.createStatement()) {

				logger.info("Take script");
				//todo MVP take only one script
				String unfeeledSQL = taskProcessorConfig.getScripts().get(0);
				/*for (String unfeeledSQL: taskProcessorConfig.getScripts() ) {
				*/
				logger.info("resolve table changes");
				resolveTable(connection);


				logger.info("Feel script");


			String columns =
			taskProcessorConfig
					.getTargetDataSet()
					.getColumnSchema()
					.stream()
					.filter(columnDTO -> !columnDTO.getDataType().equalsIgnoreCase("serial"))
					.map(ColumnDTO::getName)
					.collect(Collectors.joining(", "));

			String sql = String.format(
					"insert into %s (%s)\n select %s\n from (\n%s\n)",
					taskProcessorConfig.getTargetDataSet().getFullTableName(),
					columns,
					columns,
					feelScripts(unfeeledSQL));
				logger.info("Creating statement...{}", sql);
				Boolean isRetrieved = statement.execute(sql);
				logger.info("Script execution is done");



			}
			catch (SQLException e) {
				logger.error(e.getMessage());
				throw new RuntimeException(e);
			}

		}catch (Exception e){
			logger.error("Error task execution",e);
			return Status.Task.FAILED;
		}
		return Status.Task.SUCCESS;
    }

	public String feelScripts(String script){
		String result = new String(script);

		for(Map.Entry<String,String> sse: taskProcessorConfig
				.getKeyBind()
				.entrySet()){
			logger.info("Replace {} to {}",sse.getKey(),sse.getValue());
			result = result.replace(sse.getKey(), sse.getValue());
		}
		logger.info("Prepared query looks as {}", result);
		return result;
	}

	private void resolveTable(Connection connection) throws SQLException {
		String[] table = taskProcessorConfig.getTargetDataSet().getFullTableName().split("\\.");

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
			createSchema(connection,table[0]);
		logger.info("Check table");
		if (!isObjectExists(connection,checkSQLTable))
			createTable(connection);

	}

	private void createSchema(Connection connection, String schemaName) throws SQLException {
		logger.info("Create schema {}",schemaName);
		executeDDL(connection, String.format("create schema %s" , schemaName));
	}

	private void executeDDL(Connection connection, String ddl) throws SQLException {
		logger.info("Execute ddl {}",ddl);
		Statement statement = connection.createStatement();
		statement.execute(ddl);
		statement.close();

	}
	private void createTable(Connection connection) throws SQLException {
		logger.info("Create table");
		StringJoiner columns = new StringJoiner(",");

		taskProcessorConfig.getTargetDataSet().getColumnSchema().stream().map(columnDTO ->
				String.format("%s %s", columnDTO.getName(), columnDTO.getDataType())).forEach(columns::add);

		String createTable = String.format(
				"create table %s (%s)",
				taskProcessorConfig.getTargetDataSet().getFullTableName(),
				columns.toString()
				// todo storage parameters ?
				// todo constraints
		);

		executeDDL(connection,createTable);
	}
	private boolean isObjectExists(Connection connection, String checkSQL){
		boolean result = false;




		try (Statement statement = connection.createStatement();
				ResultSet rs = statement.executeQuery(checkSQL);){

			while (rs.next()) {
				result = rs.getBoolean(1);
			}

		} catch (SQLException e) {
			logger.warn(e.fillInStackTrace().toString());
			return false;
		}
		return result;

	}


	private boolean isTableExists(Connection connection)  {
		String tableName = taskProcessorConfig.getTargetDataSet().getFullTableName();

		logger.info("Check table {} exits ", tableName);
		return isObjectExists(connection, String.format("select * from %s where 1=2", tableName));
		//Statement statement = null;

		/*try {
			statement = connection.createStatement();
		} catch (SQLException e) {
            logger.warn("Can't create statement");
            logger.warn(e.fillInStackTrace().toString());
			return false;
        }
       try {
            assert statement != null;
            return statement.execute(String.format("select * from %s where 1=2", tableName));
		} catch (SQLException e) {
			logger.warn(e.fillInStackTrace().toString());
			return false;

		}*/
    }
}
