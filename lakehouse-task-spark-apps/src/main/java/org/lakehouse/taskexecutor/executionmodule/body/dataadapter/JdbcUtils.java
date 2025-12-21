package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import com.amazonaws.services.dynamodbv2.xspec.S;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.constant.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class JdbcUtils {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String jdbcUrlTemplate = "jdbc:${jdbcType}://${host}:${port}/${urn}";
    public void execute(String sql, Map<String, String> options) throws SQLException {
        Connection connection = null;
        try {
            connection = JdbcConnectionFactory.getConnection(options);
            logger.info("Execute SQL command: {}", sql);
            connection.createStatement().execute(sql);
        } catch (SQLException e) {
            logger.info(e.getLocalizedMessage());
            throw e;
        }finally {
            if( connection != null)  connection.close();
        }
    }

    public String getConnectionString(DataSourceDTO dataSourceDTO){
        return jdbcUrlTemplate
                .replace("${jdbcType}",getReMappedEngine(dataSourceDTO.getEngine()))
                .replace("${host}",dataSourceDTO.getServices().get(0).getHost())
                .replace("${port}",dataSourceDTO.getServices().get(0).getPort())
                .replace("${urn}",dataSourceDTO.getServices().get(0).getUrn());
    }
    public Map<String,String> dtoToProps(DataSourceDTO dataSourceDTO){
        Map<String,String> result = new HashMap<>();
        result.putAll(dataSourceDTO.getProperties());
        result.putAll(dataSourceDTO.getServices().get(0).getProperties());
        result.put("url", getConnectionString(dataSourceDTO));
        return result;
    }

    public final Map<Types.Engine,String> jdbcEngineReMap = Map.of(Types.Engine.postgres,"postgresql");

    public final String getReMappedEngine (Types.Engine engine){
        if (jdbcEngineReMap.containsKey(engine))
            return jdbcEngineReMap.get(engine);
        else return engine.label;
    }
}
