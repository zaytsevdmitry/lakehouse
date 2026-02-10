package org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.exception.*;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.execute.SparkExecuteUtils;
import org.lakehouse.taskexecutor.executionmodule.body.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class SparkSQLDataSourceManipulatorAbstract implements DataSourceManipulator {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SparkSQLDataSourceManipulatorParameter sparkSQLDataSourceManipulatorParameter;



    private final Map<String,Object> classContext = new HashMap<>();
    public SparkSQLDataSourceManipulatorAbstract(SparkSQLDataSourceManipulatorParameter sparkSQLDataSourceManipulatorParameter) {
        this.sparkSQLDataSourceManipulatorParameter = sparkSQLDataSourceManipulatorParameter;
        classContext.put(SystemVarKeys.CURRENT_DATASET_KEY_NAME, dataSetDTO().getKeyName());

    }

    @Override
    public void createTableIfNotExists() throws CreateException {
        String tableName = getCatTableName();
        logger.info("Try to create table {}",tableName);
        boolean isTableExists = sparkSession().catalog().tableExists(tableName);

        if(!isTableExists){
            String schemaName = getCatDbSchemaName();
            logger.info("Try to create table {}", schemaName);
            boolean isDBSchemaExists = sparkSession().catalog().databaseExists(schemaName);

            try {

                if (!isDBSchemaExists) {
                    executeUtils().execute(sqlTemplateDTO().getDatabaseSchemaDDLCreate(), classContext);
                }

                executeUtils().execute(sqlTemplateDTO().getTableDDLCreate(), classContext);
            }catch (ExecuteException e){
                throw new CreateException(e);
            }
         }
    }
    public String getCatTableName(){
            return getCatDbSchemaName() + "." +
                    dataSetDTO().getTableName();
    }
    public String getCatDbSchemaName(){
        return dataSetDTO().getDataSourceKeyName() + "." +
                dataSetDTO().getDatabaseSchemaName();
    }




    public SparkSession sparkSession() {
        return sparkSQLDataSourceManipulatorParameter.sparkSession();
    }
    @Override
    public void drop() throws DropException {
        try {
            executeUtils().execute(sqlTemplateDTO().getTableDDLDrop(), classContext);
        } catch (ExecuteException e) {
            throw new DropException(e);
        }
    }


    @Override
    public void addConstraints(Map<String, DataSetConstraintDTO> constraints) throws ConstraintException {
        throw new ConstraintException("Unsupported");
    }

    @Override
    public void compact() throws CompactException {
        try {
            executeUtils().execute(sqlTemplateDTO().getTableDDLCompact());
        } catch (ExecuteException e) {
            throw new CompactException(e);
        }
    }

    private void partitionOperation(String template,String partition) throws ExecuteException {
        Map<String, Object> context = new HashMap<>();
        context.putAll(classContext);
        context.put(SystemVarKeys.PARTITION_NAME, partition);
        executeUtils().execute(sqlTemplateDTO().getPartitionDDLDrop(), context);
    }
    @Override
    public void compactPartitions(List<String> partitions) throws CompactException {
        try {
            for(String partition:partitions) {
                partitionOperation(sqlTemplateDTO().getPartitionDDLCompact(),partition);
            }
        } catch (ExecuteException e) {
            throw new CompactException(e);
        }
    }

    @Override
    public void dropPartitions(List<String> partitions) throws DropException {
        try {
            for(String partition:partitions) {
                partitionOperation(sqlTemplateDTO().getPartitionDDLDrop(), partition);
            }
        } catch (ExecuteException e) {
            throw new DropException(e);
        }
    }

    @Override
    public void removeConstraintByName(String constraintName) throws ConstraintException {
        throw new ConstraintException("Unsupported");
    }

    @Override
    public void removeConstraints(Map<String, DataSetConstraintDTO> constraints) throws ConstraintException {
        throw new ConstraintException("Unsupported");
    }

    @Override
    public void truncate() throws TruncateException {
        try {
            executeUtils().execute(sqlTemplateDTO().getTableDDLTruncate());
        } catch (ExecuteException e) {
            throw new TruncateException(e);
        }
    }

    @Override
    public void truncatePartitions(List<String> partitions) throws TruncateException {
        try {
            for(String partition:partitions) {
                partitionOperation(sqlTemplateDTO().getPartitionDDLTruncate(),partition);
            }
        } catch (ExecuteException e) {
            throw new TruncateException(e);
        }
    }

    @Override
    public DataSetDTO dataSetDTO() {
        return sparkSQLDataSourceManipulatorParameter.dataSetDTO();
    }

    @Override
    public SparkExecuteUtils executeUtils() {
        return sparkSQLDataSourceManipulatorParameter.executeUtils();
    }

    @Override
    public SQLTemplateDTO sqlTemplateDTO() {
        return sparkSQLDataSourceManipulatorParameter.sqlTemplateDTO();
    }
}
