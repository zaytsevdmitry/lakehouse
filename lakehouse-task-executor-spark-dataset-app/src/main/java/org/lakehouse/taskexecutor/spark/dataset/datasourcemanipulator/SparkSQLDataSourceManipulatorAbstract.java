package org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator;

import org.apache.spark.sql.SparkSession;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.taskexecutor.api.datasource.DataSourceManipulator;
import org.lakehouse.taskexecutor.api.datasource.exception.*;
import org.lakehouse.taskexecutor.api.facade.SQLTemplateResolver;
import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.execute.SparkExecuteUtils;
import org.lakehouse.taskexecutor.spark.dataset.datasourcemanipulator.parameter.SparkSQLDataSourceManipulatorParameter;
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

        try {
            logger.info("Try to create database schema {}", dataSetDTO().getDatabaseSchemaName());
            executeUtils().execute(sqlTemplateResolver().getDatabaseSchemaDDLCreate(), classContext);
            logger.info("Try to create table {}",dataSetDTO().getTableName());
            executeUtils().execute(sqlTemplateResolver().getTableDDLCreate(), classContext);
        }catch (ExecuteException e){
            throw new CreateException(e);
        }
    }

    public SparkSession sparkSession() {
        return sparkSQLDataSourceManipulatorParameter.sparkSession();
    }
    @Override
    public void drop() throws DropException {
        try {
            executeUtils().execute(sqlTemplateResolver().getTableDDLDrop(), classContext);
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
            executeUtils().execute(sqlTemplateResolver().getTableDDLCompact());
        } catch (ExecuteException e) {
            throw new CompactException(e);
        }
    }

    private void partitionOperation(String template,String partition) throws ExecuteException {
        Map<String, Object> context = new HashMap<>();
        context.putAll(classContext);
        context.put(SystemVarKeys.PARTITION_NAME, partition);
        executeUtils().execute(sqlTemplateResolver().getPartitionDDLDrop(), context);
    }
    @Override
    public void compactPartitions(List<String> partitions) throws CompactException {
        try {
            for(String partition:partitions) {
                partitionOperation(sqlTemplateResolver().getPartitionDDLCompact(),partition);
            }
        } catch (ExecuteException e) {
            throw new CompactException(e);
        }
    }

    @Override
    public void dropPartitions(List<String> partitions) throws DropException {
        try {
            for(String partition:partitions) {
                partitionOperation(sqlTemplateResolver().getPartitionDDLDrop(), partition);
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
            executeUtils().execute(sqlTemplateResolver().getTableDDLTruncate());
        } catch (ExecuteException e) {
            throw new TruncateException(e);
        }
    }

    @Override
    public void truncatePartitions(List<String> partitions) throws TruncateException {
        try {
            for(String partition:partitions) {
                partitionOperation(sqlTemplateResolver().getPartitionDDLTruncate(),partition);
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
    public SQLTemplateResolver sqlTemplateResolver() {
        return sparkSQLDataSourceManipulatorParameter.sqlTemplateResolver();
    }
}
