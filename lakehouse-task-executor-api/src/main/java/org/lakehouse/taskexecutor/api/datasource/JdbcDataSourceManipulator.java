package org.lakehouse.taskexecutor.api.datasource;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetConstraintDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.utils.Coalesce;
import org.lakehouse.taskexecutor.api.datasource.exception.*;
import org.lakehouse.taskexecutor.api.datasource.execute.ExecuteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcDataSourceManipulator implements DataSourceManipulator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExecuteUtils executeUtils;
    private final SQLTemplateDTO sqlTemplateDTO;
    private final DataSetDTO dataSetDTO;
    public JdbcDataSourceManipulator(
            DataSourceManipulatorParameter dataSourceManipulatorParameter) {
        this.executeUtils = dataSourceManipulatorParameter.executeUtils();
        this.sqlTemplateDTO = dataSourceManipulatorParameter.sqlTemplateDTO();
        this.dataSetDTO = dataSourceManipulatorParameter.dataSetDTO();
    }

    @Override
    public ExecuteUtils executeUtils() {
        return executeUtils;
    }

    @Override
    public SQLTemplateDTO sqlTemplateDTO() {
        return sqlTemplateDTO;
    }



    @Override
    public void createTableIfNotExists() throws CreateException {

        try {
            executeUtils().executeIfFalse(
                    sqlTemplateDTO().getDatabaseSchemaExistsSQL(),
                    sqlTemplateDTO().getDatabaseSchemaDDLCreate(),
                    new HashMap<>());

            executeUtils().executeIfFalse(
                    sqlTemplateDTO().getTableSQLExists(),
                    sqlTemplateDTO().getTableDDLCreate(),
                    new HashMap<>());


        } catch (ExecuteException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void drop() throws DropException {
        try {
            executeUtils.executeIfFalse(
                    sqlTemplateDTO().getTableSQLExists(),
                    sqlTemplateDTO().getTableSQLExists(),
                    new HashMap<>());

        } catch ( ExecuteException  e) {
            throw new DropException(e);
        }
    }

    @Override
    public void truncate() throws TruncateException {
        try {
            executeUtils.execute(sqlTemplateDTO().getTableDDLTruncate(),new HashMap<>());
        } catch (ExecuteException e) {
            throw new TruncateException(e);
        }
    }
    private void executePartitions(
            String template,
            List<String> partitions) throws  ExecuteException {
        Map<String,Object> localContext = new HashMap<>();
        for(String partition:partitions){
            localContext.put(SystemVarKeys.PARTITION_NAME, partition);
            executeUtils.execute(template,localContext);
        }

    }

    @Override
    public void dropPartitions( List<String> partitions) throws DropException {
        try {
            executePartitions(
                sqlTemplateDTO().getPartitionDDLDrop(),
                partitions);
        } catch (ExecuteException  e) {
                throw new DropException(e);
        }
    }

    @Override
    public void truncatePartitions( List<String> partitions) throws TruncateException {
        try {
            executePartitions(sqlTemplateDTO().getPartitionDDLTruncate(), partitions);
        } catch (ExecuteException e) {
            throw new TruncateException(e);
        }
    }

    private void executeConstraint(
            String constraintName,
            String template) throws JsonProcessingException,ExecuteException {
        executeUtils().execute(template,Map.of(SystemVarKeys.CONSTRAINT_NAME, constraintName));
    }
    @Override
    public void removeConstraints(Map<String,DataSetConstraintDTO> constraints) throws ConstraintException {
        for (String constraintName: constraints
                .entrySet()
                .stream()
                .filter(e -> e.getValue().getConstraintLevelCheck().equals(Types.ConstraintLevelCheck.construct))
                .map(Map.Entry::getKey)
                .toList()){
            try {
                executeConstraint(constraintName, sqlTemplateDTO().getConstraintDDLDrop());
            } catch (JsonProcessingException | ExecuteException e) {
                throw new ConstraintException(e);
            }
        }
    }

    @Override
    public void removeConstraintByName( String constraintName) throws ConstraintException {
        try {
            executeConstraint(constraintName, sqlTemplateDTO().getConstraintDDLDrop());
        } catch (JsonProcessingException | ExecuteException e) {
            throw new ConstraintException(e);
        }
    }

    private String getConstraintTemplate(Map.Entry<String,DataSetConstraintDTO> constraint) throws ConstraintException {
        String template = null;
        if ( constraint.getValue().getType().equals(Types.Constraint.primary))
            template = sqlTemplateDTO().getPrimaryKeyDDLAdd();
        else if (constraint.getValue().getType().equals(Types.Constraint.unique))
            template = sqlTemplateDTO().getUniqueKeyDDLAdd();
        else if (constraint.getValue().getType().equals(Types.Constraint.foreign))
            template = sqlTemplateDTO().getForeignKeyDDLAdd();
        else if (constraint.getValue().getType().equals(Types.Constraint.check))
            template = sqlTemplateDTO().getCheckConstraintDDLAdd();
        else throw new
                    ConstraintException(
                    String.format(
                            "Unknown type of constraint %s",
                            constraint.getValue().getType().label));
        return Coalesce.apply(
                constraint.getValue().getTableConstraintDDLAddOverride(),
                template);
    }

    @Override
    public void addConstraints(Map<String,DataSetConstraintDTO> constraints) throws ConstraintException {

        for (Map.Entry<String,DataSetConstraintDTO> constraint:constraints
                .entrySet()
                .stream()
                .filter(e ->
                        e.getValue().getConstraintLevelCheck().equals(Types.ConstraintLevelCheck.construct))
                .toList()){
            try {
                String template = getConstraintTemplate(constraint);

                executeConstraint(constraint.getKey(),template);
            } catch (JsonProcessingException | ExecuteException e) {
                throw new ConstraintException(e);
            }
        }
    }

    @Override
    public void compact() throws CompactException {

    }

    @Override
    public void compactPartitions(List<String> partitions) throws CompactException {

    }
    @Override
    public DataSetDTO dataSetDTO() {
        return dataSetDTO;
    }
}
