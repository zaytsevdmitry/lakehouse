/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lakehouse.taskexecutor.api.datasource.execute.jdbc;

import org.lakehouse.client.api.utils.CollectionFactory;
import org.lakehouse.security.SecretResolver;
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
        // Resolve the password via the configured SecretProvider (if any) and strip
        // the security subsystem keys before passing options down to the JDBC driver
        Map<String, String> resolved = SecretResolver.resolveAndSanitize(options);
        String url = resolved.get(URL_KEY);
        Properties info = CollectionFactory
                .mapToProperties(
                        resolved
                                .entrySet()
                                .stream()
                                .filter(se -> !se.getKey().equals(URL_KEY))
                                .collect(Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue)));
        logger.info("Trying to connect to {}", URL_KEY);
        Connection conn = DriverManager.getConnection(url, info);
        logger.info("Connection to {} established successfully", url);
        return conn;
    }
}
