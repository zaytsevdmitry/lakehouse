package org.lakehouse.taskexecutor.api.datasource.execute;

import com.hubspot.jinjava.Jinjava;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;
import org.lakehouse.client.api.dto.configs.datasource.ServiceDTO;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.taskexecutor.api.datasource.exception.ExecuteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class ExecuteUtilsAbstract implements ExecuteUtils {
    public static String RESULT_COLUMN_NAME = "result";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final Jinjava jinjava;
    private final DataSourceDTO dataSourceDTO;
    private final DriverDTO driverDTO;
    public ExecuteUtilsAbstract(Jinjava jinjava, DataSourceDTO dataSourceDTO, DriverDTO driverDTO) {
        this.jinjava = jinjava;
        this.dataSourceDTO = dataSourceDTO;
        this.driverDTO = driverDTO;
    }

    public Jinjava getJinjava() {
        return jinjava;
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

        boolean isExists = 1 == executeGetResultInt(checkQuery,localContext);
        if(!isExists)
            execute(executionQuery,localContext);
    }
    @Override
    public void executeIfFalse(
            String checkQuery,
            String executionQuery,
            Map<String,Object> localContext) throws ExecuteException {
        logger.info("Check query {}\n if true query {}",checkQuery,executionQuery);
        boolean isExists = 1 == executeGetResultInt(checkQuery,localContext);
        if(!isExists)
            execute(executionQuery,localContext);
    }

    /**
     * @return String value of template of type Types.ConnectionType.jdbc
     * */

    public String getConnectionString() throws TaskConfigurationException {
        if (dataSourceDTO.getService()==null){
            throw new TaskConfigurationException(String.format("DataSource %s with empty list of services", dataSourceDTO.getKeyName()));
        }
        return jinjava.render(
                driverDTO.getConnectionTemplates().get(Types.ConnectionType.jdbc),
                Map.of("service",dataSourceDTO.getService())
        );
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
