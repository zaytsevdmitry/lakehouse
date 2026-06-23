/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.lakehouse.taskexecutor.api.datasource.execute.jdbc;

import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.jinja.java.JinJavaUtils;
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
            JinJavaUtils jinJavaUtils,
            DataSourceDTO dataSourceDTO,
            DriverDTO driverDTO
            ) {
        super(jinJavaUtils, dataSourceDTO, driverDTO);
    }

    

    public Connection getConnection() throws TaskConfigurationException,ExecuteException {
        Map<String,String> props = dtoToProps();
        try {
            return JdbcConnectionFactory.getConnection(props);
        } catch (SQLException e) {
            throw new ExecuteException(e);
        }
    }

    @Override
    public void execute(String sql, Map<String,Object> localContext) throws ExecuteException {
        logger.info("Execute SQL command: {}", sql);

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            String renderedSQL = getjinJavaUtils().render(sql,localContext);
            logger.info("Execute renderedSQL command: {}", renderedSQL);
            statement.execute(renderedSQL);
        } catch (SQLException | TaskConfigurationException  e) {
            logger.info(e.getLocalizedMessage());
            throw new ExecuteException(e);
        }
    }
    @Override
    public void execute(String sql) throws ExecuteException {
        execute(sql, new HashMap<>());
    }

    public Object executeGetResultObject(String sql,Map<String,Object> localContext) throws ExecuteException{



        Object result = null;

        String renderedSQL = getjinJavaUtils().render(sql, localContext);

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(renderedSQL))
        {
            resultSet.next();
            result = resultSet.getObject(RESULT_COLUMN_NAME);

        } catch (SQLException | TaskConfigurationException e) {
            logger.info(e.getLocalizedMessage());
            throw new ExecuteException(e);
        }
        return result;
    }
    @Override
    public Integer executeGetResultInt(String sql,Map<String,Object> localContext) throws ExecuteException{
        return  (Integer) executeGetResultObject(sql,localContext);
    }

    @Override
    public Long executeGetResultLong(String sql, Map<String, Object> localContext) throws ExecuteException {
        return  (Long) executeGetResultObject(sql,localContext);
    }
}
