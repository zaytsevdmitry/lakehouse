package org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.jdbc;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.lakehouse.client.api.constant.Configuration;
import org.lakehouse.client.api.exception.TaskConfigurationException;
import org.lakehouse.taskexecutor.api.datasource.exception.ReadException;
import org.lakehouse.taskexecutor.api.datasource.exception.WriteException;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.SparkSQLDataSourceManipulatorAbstract;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class JdbcSparkSQLDataSourceManipulator extends SparkSQLDataSourceManipulatorAbstract {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public JdbcSparkSQLDataSourceManipulator(SparkSQLDataSourceManipulatorParameter sparkSQLDataSourceManipulatorParameter) {
        super(sparkSQLDataSourceManipulatorParameter);
        
    }

    /**
     * read data from source
     * @param tryNum - number of try to connect starts of 0
     * @return dataset
     * @throws ReadException when all tries exceed
     */
    private Dataset<Row>  readFaultTolerance(Map<String,String> options, Integer tryNum) throws ReadException {

        try {
            Map<String, String> o = new HashMap<>();
            o.putAll(executeUtils().dtoToProps(tryNum));
            o.put("dbtable", getTableName());
            return sparkSession().read().format("jdbc").options(o).load();
        }catch (TaskConfigurationException e){
            logger.error(e.getMessage());
            throw new ReadException(e.getLocalizedMessage(), e);
        }
        catch (Exception  e) {
            logger.info(e.getMessage());
            // todo use override this to use SQL ERR CODE for each database to take connection timeout
            return readFaultTolerance(options,tryNum+1);
        }
    }

    @Override
    public Dataset<Row>  read(Map<String,String> options) throws ReadException {
        return readFaultTolerance(options, 0);
    }

    @Override
    public void write(
            Dataset<Row> dataset,
            Configuration.ModificationRule modificationRule) throws WriteException {
        try {
            dataset
                    .writeTo(getCatTableName())
                    .append();
        } catch (Exception e) {
            throw new WriteException(e);
        }

    }


    public String getTableName(){
        if (getDbSchemaName().isBlank())
            return dataSetDTO().getTableName();

        return getDbSchemaName() + "." +
                dataSetDTO().getTableName();
    }

    public String getDbSchemaName(){
        return  dataSetDTO().getDatabaseSchemaName();
    }
}
