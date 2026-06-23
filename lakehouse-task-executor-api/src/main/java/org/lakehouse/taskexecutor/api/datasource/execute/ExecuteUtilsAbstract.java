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

import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.jinja.java.JinJavaUtils;
import org.lakehouse.taskexecutor.api.datasource.exception.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class ExecuteUtilsAbstract implements ExecuteUtils {
    public static String RESULT_COLUMN_NAME = "result";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final JinJavaUtils jinJavaUtils;
    private final DataSourceDTO dataSourceDTO;
    private final DriverDTO driverDTO;
    public ExecuteUtilsAbstract(JinJavaUtils jinJavaUtils, DataSourceDTO dataSourceDTO, DriverDTO driverDTO) {
        this.jinJavaUtils = jinJavaUtils;
        this.dataSourceDTO = dataSourceDTO;
        this.driverDTO = driverDTO;
    }

    public JinJavaUtils getjinJavaUtils() {
        return jinJavaUtils;
    }

    public DriverDTO getDriverDTO() {
        return driverDTO;
    }


    public DataSourceDTO getDataSourceDTO() {
        return dataSourceDTO;
    }

    @Override
    public void executeIfTrue(
            String checkQuery,
            String executionQuery,
            Map<String,Object> localContext) throws ExecuteException {
        logger.info("Check query {}\n if true query {}",checkQuery,executionQuery);

        if(1 == executeGetResultInt(checkQuery,localContext))
            execute(executionQuery,localContext);
    }
    @Override
    public void executeIfFalse(
            String checkQuery,
            String executionQuery,
            Map<String,Object> localContext) throws ExecuteException {
        logger.info("Check query {}\n if false query {}",checkQuery,executionQuery);
        if((1 != executeGetResultInt(checkQuery,localContext)))
            execute(executionQuery,localContext);
    }

    /**
     * @return String value of template of type Types.ConnectionType.jdbc
     * */

    public String getConnectionString() throws TaskConfigurationException {
        if (dataSourceDTO.getService()==null){
            throw new TaskConfigurationException(String.format("DataSource %s with empty list of services", dataSourceDTO.getKeyName()));
        }
        if (driverDTO.getConnectionTemplates().containsKey(Types.ConnectionType.jdbc))
            return jinJavaUtils.render(
                driverDTO.getConnectionTemplates().get(Types.ConnectionType.jdbc),
                Map.of(SystemVarKeys.SERVICE_KEY, dataSourceDTO.getService())
        );
        else throw new TaskConfigurationException(
                String.format(
                        "The %s %s connection template not contains '%s'",
                        DriverDTO.class.getSimpleName(), driverDTO.getKeyName(),Types.ConnectionType.jdbc.label
                         ));
    }
    public Map<String,String> dtoToProps() throws TaskConfigurationException {
        if (dataSourceDTO.getService()==null){
            throw new TaskConfigurationException(String.format("DataSource %s with empty list of services", dataSourceDTO.getKeyName()));
        }
        Map<String,String> result = new HashMap<>();
        result.putAll(dataSourceDTO.getService().getProperties());
        result.put("url", getConnectionString());
        return result;
    }

}
