package org.lakehouse.taskexecutor.api.datasource.execute.jdbc;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.taskexecutor.api.datasource.exception.ExecuteException;
import org.lakehouse.taskexecutor.api.datasource.execute.ExecuteUtilsAbstract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class JdbcExecuteUtils extends ExecuteUtilsAbstract {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());


    public JdbcExecuteUtils(
            Jinjava jinjava,
            DataSourceDTO dataSourceDTO,
            DriverDTO driverDTO
            ) {
        super(jinjava, dataSourceDTO, driverDTO);
    }

    

    private Connection retryableConnection(Integer tryNum) throws TaskConfigurationException {
        Map<String,String> props = dtoToProps(tryNum);
        try {
            return JdbcConnectionFactory.getConnection(props);
        } catch (SQLException e) {
            return retryableConnection(tryNum+1);
        }
    }

    public Connection getConnection() throws TaskConfigurationException {
        return retryableConnection(0);
    }

    @Override
    public void execute(String sql, Map<String,Object> localContext) throws ExecuteException {
        logger.info("Execute SQL command: {}", sql);

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            String renderedSQL = getJinjava().render(sql,localContext);
            logger.info("Execute renderedSQL command: {}", renderedSQL);
            statement.execute(renderedSQL);
        } catch (SQLException | TaskConfigurationException e) {
            logger.info(e.getLocalizedMessage());
            throw new ExecuteException(e);
        }
    }
    @Override
    public void execute(String sql) throws ExecuteException {
        execute(sql, new HashMap<>());
    }

    @Override
    public Integer executeGetResultInt(String sql,Map<String,Object> localContext) throws ExecuteException{



        Integer result = null;

        String renderedSQL = getJinjava().render(sql, localContext);
        logger.info("Execute SQL command: {}", renderedSQL);
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(renderedSQL))
        {
            resultSet.next();
            result = resultSet.getInt(RESULT_COLUMN_NAME);

        } catch (SQLException | TaskConfigurationException e) {
            logger.info(e.getLocalizedMessage());
            throw new ExecuteException(e);
        }
        return result;
    }

    public Map<String,String> dtoToProps(Integer tryNum) throws TaskConfigurationException {
        if (getDataSourceDTO().getServices().size() <= tryNum) {
            throw new TaskConfigurationException(
                    String.format("No more values in Service's list. Try num %d",  tryNum));
        }
        Map<String,String> result = new HashMap<>();
        result.putAll(getDataSourceDTO().getProperties());
        result.putAll(getDataSourceDTO().getServices().get(tryNum).getProperties());
        result.put("url", getConnectionString( tryNum));
        return result;
    }
}
