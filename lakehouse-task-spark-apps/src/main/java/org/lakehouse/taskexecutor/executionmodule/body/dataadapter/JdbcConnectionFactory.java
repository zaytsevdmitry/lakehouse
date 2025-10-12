package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import org.lakehouse.client.api.utils.CollectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class JdbcConnectionFactory {
    private final static Logger logger = LoggerFactory.getLogger(JdbcConnectionFactory.class);
    private final static String URL_KEY = "url";

    public static Connection getConnection(Map<String, String> options) throws SQLException {
        String url = options.get(URL_KEY);
        Properties info = CollectionFactory
                .mapToProperties(
                        options
                                .entrySet()
                                .stream()
                                .filter(se -> !se.getKey().equals(URL_KEY))
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue)));
        Connection conn = DriverManager.getConnection(url, info);
        logger.info("Connection established successfully of {}", url);
        return conn;
    }
}
