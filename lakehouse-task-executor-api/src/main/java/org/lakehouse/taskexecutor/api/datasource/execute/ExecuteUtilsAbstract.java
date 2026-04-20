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
