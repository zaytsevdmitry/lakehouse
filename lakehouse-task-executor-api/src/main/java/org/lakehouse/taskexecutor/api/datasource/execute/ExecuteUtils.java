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

package org.lakehouse.taskexecutor.api.datasource.execute;

import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.taskexecutor.api.datasource.exception.ExecuteException;

import java.util.Map;

public interface ExecuteUtils {

    /**
     * Render and execute query command without result
     * @param sql non-rendered query
     * @param localContext additional context
     * */
    void execute(String sql, Map<String,Object> localContext) throws ExecuteException;

    /**
     * Render and execute query command with empty localContext without result
     * @param sql non-rendered query
     * */
    void execute(String sql) throws ExecuteException;

    /**
     * @param checkQuery     non-rendered SQL query must return one of  values 0 or 1 where 0 its false, 1 - true
     * @param executionQuery it's  non-rendered query will be executed if checkQuery returns value 1
     * @param localContext additional context
     */
    void executeIfTrue(String checkQuery, String executionQuery, Map<String,Object> localContext) throws ExecuteException;

    /**
     * @param checkQuery     SQL query must return one of  values 0 or 1 where 0 its false, 1 - true
     * @param executionQuery it's  query will be executed if checkQuery returns value 0
     * @param localContext additional context
     */
    void executeIfFalse(String checkQuery, String executionQuery, Map<String,Object> localContext) throws ExecuteException;

    /**
     * @param sql SQL query. Must contain column with name "result"
     * @return int value of column result
     * @param localContext additional context
     */
    Integer executeGetResultInt(String sql,Map<String,Object> localContext) throws ExecuteException;
    Long executeGetResultLong(String sql,Map<String,Object> localContext) throws ExecuteException;

    String getConnectionString() throws TaskConfigurationException;

    Map<String, String> dtoToProps() throws TaskConfigurationException;

}

