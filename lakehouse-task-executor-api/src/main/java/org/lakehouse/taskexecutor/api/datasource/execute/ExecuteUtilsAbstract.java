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
     * @param tryNum equal from list of services index
     * @return String value of template of type Types.ConnectionType.jdbc
     * */

    public String getConnectionString(Integer tryNum) throws TaskConfigurationException {
        ServiceDTO serviceDTO = null;
        if (dataSourceDTO.getServices().isEmpty()){
            throw new TaskConfigurationException(String.format("DataSource %s with empty list of services", dataSourceDTO.getKeyName()));
        }
        else {
            if (dataSourceDTO.getServices().size() >= tryNum) {
                serviceDTO = dataSourceDTO.getServices().get(tryNum);
            }
            else{
                throw new TaskConfigurationException(String.format("Insufficient list of DataSource %s services", dataSourceDTO.getKeyName()));
            }
        }
        return jinjava.render(driverDTO.getConnectionTemplates().get(Types.ConnectionType.jdbc), Map.of("service",serviceDTO));
    }
    public Map<String,String> dtoToProps(Integer tryNum) throws TaskConfigurationException {
        if (dataSourceDTO.getServices().size() < tryNum) {
            throw new TaskConfigurationException(
                    String.format("No more values in Service's list. Try num %d",  tryNum));
        }
        Map<String,String> result = new HashMap<>();
        result.putAll(dataSourceDTO.getProperties());
        result.putAll(dataSourceDTO.getServices().get(tryNum).getProperties());
        result.put("url", getConnectionString(tryNum));
        return result;
    }

}
