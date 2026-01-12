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

    String getConnectionString(Integer tryNum) throws TaskConfigurationException;

    Map<String, String> dtoToProps(Integer tryNum) throws TaskConfigurationException;

}

