package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public class JdbcUtils {
    private final Connection connection;

    public JdbcUtils(Connection connection) {
        this.connection = connection;
    }

    public void execute(String sql, Map<String, String> options) throws SQLException {
        Connection connection = JdbcConnectionFactory.getConnection(options);
        connection.createStatement().execute(sql);
    }
}
