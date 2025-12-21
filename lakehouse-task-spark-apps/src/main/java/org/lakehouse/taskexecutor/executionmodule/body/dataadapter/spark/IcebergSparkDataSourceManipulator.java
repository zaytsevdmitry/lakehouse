package org.lakehouse.taskexecutor.executionmodule.body.dataadapter.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.lakehouse.client.api.constant.Configuration;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.DataSourceManipulatorParameter;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.CreateException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.DropException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.ReadException;
import org.lakehouse.taskexecutor.executionmodule.body.dataadapter.exception.WriteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public  class IcebergSparkDataSourceManipulator extends FileSparkDataSourceManipulatorAbstract {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static boolean isCompatible(
            Types.EngineType type,
            Types.Engine serviceType){
        return type.equals(Types.EngineType.spark);
    }
    public IcebergSparkDataSourceManipulator(
            DataSourceManipulatorParameter dataSourceManipulatorParameter) {
        super(dataSourceManipulatorParameter);
       }

    private boolean isTableExists(){
        log.info("Check exists iceberg table {}", getCatalogTableFullName());
        System.out.println(getSparkSession().conf().getAll());
        return getSparkSession()
                .catalog()
                .tableExists(
                        getCatalogTableFullName());
    }

    @Override
    public Dataset<Row> read(Map<String, String> options) throws ReadException {
        return getSparkSession().table(getCatalogTableFullName());
    }

    @Override
    public void createIfNotExists() throws CreateException{
        SparkSession sparkSession =getSparkSession();

        // sparkSession.sql(String.format("use %s", getDataSourceDTO().getKeyName()));
        // todo create database unlike "default"
        if (!isTableExists()){
            String fullTableName = getCatalogTableFullName();
            String columns = getTableDialect().getColumnsDDL();
            log.info("Create iceberg table {}", fullTableName);
            sparkSession.sql(
              "CREATE TABLE "+ fullTableName  + " (\n" +
                      columns +
                      ")\n" +
                      "USING iceberg;"
            );
        } else {
            log.info("Already exists iceberg table {}", getCatalogTableFullName());
        }
    }

    @Override
    public void drop() throws DropException {
        getSparkSession().sql("DROP table IF EXISTS " + getCatalogTableFullName() + " PURGE").show();
    }

    @Override
    public void dropPartitions(String location, List<String> partitions, Map<String, String> options) throws DropException {
        throw new DropException("Unsupported operation");
    }

    @Override
    public void write(Dataset<Row> dataset, Map<String, String> options, Configuration.ModificationRule modificationRule) throws WriteException {
        //super.write(dataset, options, modificationRule);
        log.info("Write to iceberg table {}", getCatalogTableFullName());

        try {
            createIfNotExists();
            dataset.writeTo(getCatalogTableFullName()).append();
        }catch (NoSuchTableException | CreateException e){
            throw new WriteException(e);
        }
    }

}
