package org.lakehouse.taskexecutor.executionmodule;

import org.lakehouse.client.api.dto.configs.DataStoreDTO;
import org.lakehouse.taskexecutor.entity.TaskProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class JdbcTaskProcessor extends AbstractTaskProcessor{
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	public JdbcTaskProcessor(
			TaskProcessorConfig taskProcessorConfig) {
		super(taskProcessorConfig);
	}

	@Override
	public void run() {



		DataStoreDTO ds = getTaskProcessorConfig()
				.getDataStores()
				.get(getTaskProcessorConfig()
						.getTargetDataSet()
						.getDataStore());

		Properties properties = new Properties();
	    properties.putAll(ds.getProperties());
		logger.info("Registering JDBC driver...key driver");
        try {
            Class.forName(ds.getDriverClassName());

			logger.info("Connecting to database...");
			try(
					Connection connection = DriverManager.getConnection(ds.getUrl(),properties);
					Statement statement = connection.createStatement()) {

				for (String sql: getTaskProcessorConfig().getScripts() ) {
					logger.info("Creating statement...{}", sql);
					Boolean isRetrieved = statement.execute(sql);
                    logger.info("Is data retrieved: {}", isRetrieved);
				}
			}
			catch (SQLException e) {
				throw new RuntimeException(e);
			}

		} catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }


    }

}
