package org.lakehouse.taskexecutor.executionmodule.body.dataadapter;

import com.hubspot.jinjava.Jinjava;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.factory.dialect.TableDialect;
import org.lakehouse.jinja.java.JinJavaFactory;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.ReadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class SparkDataSourceManipulatorAbstract implements DataSourceManipulator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final DataSourceManipulatorParameter dataSourceManipulatorParameter;
    private final TableDialect tableDialect;
    private final Jinjava jinjava = new JinJavaFactory().getJinjava();
    public static boolean isCompatible(
            Types.EngineType type,
            Types.Engine serviceType){
        return type.equals(Types.EngineType.spark);
    }

    public SparkDataSourceManipulatorAbstract(DataSourceManipulatorParameter dataSourceManipulatorParameter) {
        this.dataSourceManipulatorParameter = dataSourceManipulatorParameter;
        tableDialect = dataSourceManipulatorParameter.getTableDialect();
    }

    @Override
    public Dataset<Row> read(Map<String, String> options) throws ReadException {
        Map<String, String> o = new HashMap<>();
        o.putAll(dataSourceManipulatorParameter.getDataSourceDTO().getProperties());
        o.putAll(options);
        try {
            return dataSourceManipulatorParameter
                    .getSparkSession()
                    .read()
                    .format(getFormat()).options(o)
                    .load();
        } catch (Exception e) {
            String msg = "Error when read dataSet "
                    + getDataSetDTO().toString();

            throw new ReadException(msg,e);
        }
    }



    private Dataset<Row> executeQueryWithPushDown(String query){
        throw new UnsupportedOperationException();
    }
    private Dataset<Row> executeQueryWithOutPushDown(String query){

        logger.info("query {}", query);
        return dataSourceManipulatorParameter.getSparkSession().sql(query);
    }
    @Override
    public Dataset<Row> executeQuery(String query,  boolean enablePushDown) {
        if(enablePushDown){
           return executeQueryWithOutPushDown(query);
        }else{
           return executeQueryWithOutPushDown(query);
        }
    }

    public String getFormat() throws Exception {
        if (getDataSourceDTO().getEngineType().equals(Types.EngineType.database))
            return "jdbc";
        else if (getDataSourceDTO().getEngineType().equals(Types.EngineType.spark))
            return getDataSourceDTO().getEngine().label;
        else
            throw new Exception("Spark format {} unexpected");

    }

    public SparkSession getSparkSession() {
        return dataSourceManipulatorParameter.getSparkSession();
    }

    public DataSourceDTO getDataSourceDTO() {
        return dataSourceManipulatorParameter.getDataSourceDTO();
    }

    public DataSetDTO getDataSetDTO() {
        return dataSourceManipulatorParameter.getDataSetDTO();
    }

    public String getCatalogTableFullName(){
        String schemaName = getTableDialect().getDatabaseSchemaName();
        if (schemaName == null || schemaName.isBlank()){
            schemaName = "default";
        }
        String result =
                getDataSetDTO().getDataSourceKeyName() + "." +
                        schemaName + "." +
                        tableDialect.getTableName();
        logger.info("CatalogTableFullName is {}",result);
        return result;
    }

    @Override
    public TableDialect getTableDialect() {
        return tableDialect;
    }
    @Override
    public Map<String, String> getKeyBind() {
        return dataSourceManipulatorParameter.getKeyBind();
    }

}
