package org.lakehouse.taskexecutor.api.facade;

import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.common.SQLTemplateGetter;
import org.lakehouse.client.rest.config.ConfigRestClientApi;

public class SQLTemplateResolver implements SQLTemplateGetter {
    private final ConfigRestClientApi configRestClientApi;
    private final SQLTemplateDTO sqlTemplateDTO;
    public SQLTemplateResolver(ConfigRestClientApi configRestClientApi, SQLTemplateDTO sqlTemplateDTO) {
        this.configRestClientApi = configRestClientApi;
        this.sqlTemplateDTO = sqlTemplateDTO;
    }
    @Override
    public String getDatabaseSchemaName() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getDatabaseSchemaName());
    }


    @Override
    public String getDatabaseSchemaDDLCreate() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getDatabaseSchemaDDLCreate());
    }


    @Override
    public String getDatabaseSchemaExistsSQL() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getDatabaseSchemaExistsSQL());
    }


    @Override
    public String getTableFullName() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getTableFullName());
    }


    @Override
    public String getTableDDLCreate() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getTableDDLCreate());
    }


    @Override
    public String getTableSQLExists() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getTableSQLExists());
    }


    @Override
    public String getTableDDLDrop() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getTableDDLDrop());
    }


    @Override
    public String getTableDDLTruncate() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getTableDDLTruncate());
    }


    @Override
    public String getTableDDLCompact() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getTableDDLCompact());
    }


    @Override
    public String getColumnNonNullCheckIntegrity() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getColumnNonNullCheckIntegrity());
    }


    @Override
    public String getColumnCheckIntegrity() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getColumnCheckIntegrity());
    }


    @Override
    public String getPartitionDDLExchange() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getPartitionDDLExchange());
    }


    @Override
    public String getPartitionDDLDrop() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getPartitionDDLDrop());
    }


    @Override
    public String getPartitionDDLTruncate() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getPartitionDDLTruncate());
    }


    @Override
    public String getPartitionDDLAdd() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getPartitionDDLAdd());
    }


    @Override
    public String getPartitionDDLCompact() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getPartitionDDLCompact());
    }


    @Override
    public String getConstraintDDLDrop() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getConstraintDDLDrop());
    }


    @Override
    public String getPrimaryKeyDDL() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getPrimaryKeyDDL());
    }


    @Override
    public String getPrimaryKeyDDLAdd() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getPrimaryKeyDDLAdd());
    }


    @Override
    public String getPrimaryKeyCheckIntegrity() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getPrimaryKeyCheckIntegrity());
    }


    @Override
    public String getForeignKeyDDL() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getForeignKeyDDL());
    }


    @Override
    public String getForeignKeyDDLAdd() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getForeignKeyDDLAdd());
    }


    @Override
    public String getForeignKeyCheckIntegrity() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getForeignKeyCheckIntegrity());
    }


    @Override
    public String getUniqueKeyDDL() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getUniqueKeyDDL());
    }


    @Override
    public String getUniqueKeyDDLAdd() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getUniqueKeyDDLAdd());
    }


    @Override
    public String getUniqueKeyCheckIntegrity() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getUniqueKeyCheckIntegrity());
    }


    @Override
    public String getColumnsCastDML() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getColumnsCastDML());
    }


    @Override
    public String getMergeDML() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getMergeDML());
    }


    @Override
    public String getInsertDML() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getInsertDML());
    }


    @Override
    public String getCheckConstraintDDL() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getCheckConstraintDDL());
    }


    @Override
    public String getCheckConstraintDDLAdd() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getCheckConstraintDDLAdd());
    }


    @Override
    public String getCheckConstraintCheckIntegrity() {
        return  configRestClientApi.getScript(sqlTemplateDTO.getCheckConstraintCheckIntegrity());
    }
}
