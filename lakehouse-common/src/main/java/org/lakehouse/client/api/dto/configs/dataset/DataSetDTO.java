package org.lakehouse.client.api.dto.configs.dataset;

import org.lakehouse.client.api.dto.common.SQLTemplateDTO;
import org.lakehouse.client.api.dto.configs.ScriptReferenceDTO;

import java.io.Serializable;
import java.util.*;

public class DataSetDTO implements Serializable {

    /*
    * TODO LIST
    *  Constraint check need "condition"
    *  Constraint unique/foreign optionally  need index
    *  Some column groups optionally  need index
    *  Some datasource's optionally need storage statement
    *       storage statement may contents projections , tablets , buckets , partitions and etc
    * */

    private String keyName;
    private String nameSpaceKeyName;
    private String dataSourceKeyName;
    private String databaseSchemaName;
    private String tableName;

    private Map<String,DataSetSourceDTO> sources = new HashMap<>();
    private List<ColumnDTO> columnSchema = new ArrayList<>();
    private Map<String, String> properties = new HashMap<>();
    private List<ScriptReferenceDTO> scripts = new ArrayList<>();
    private String description;
    private Map<String, DataSetConstraintDTO> constraints = new HashMap<>();
    private SQLTemplateDTO sqlTemplate = new SQLTemplateDTO();
    private String partitionStmt;

    public DataSetDTO() {
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getNameSpaceKeyName() {
        return nameSpaceKeyName;
    }

    public void setNameSpaceKeyName(String nameSpaceKeyName) {
        this.nameSpaceKeyName = nameSpaceKeyName;
    }

    public String getDataSourceKeyName() {
        return dataSourceKeyName;
    }

    public void setDataSourceKeyName(String dataSourceKeyName) {
        this.dataSourceKeyName = dataSourceKeyName;
    }

    public Map<String,DataSetSourceDTO> getSources() {
        return sources;
    }

    public void setSources(Map<String,DataSetSourceDTO> sources) {
        this.sources = sources;
    }

    public List<ColumnDTO> getColumnSchema() {
        return columnSchema;
    }

    public void setColumnSchema(List<ColumnDTO> columnSchema) {
        List<ColumnDTO> result = new ArrayList<>();

        columnSchema
                .stream()
                .filter(columnDTO -> columnDTO.getOrder() != null)
                .sorted(Comparator.comparing(ColumnDTO::getOrder))
                .forEach(result::add);
        columnSchema
                .stream()
                .filter(columnDTO -> columnDTO.getOrder() == null)
                .sorted(Comparator.comparing(ColumnDTO::getName))
                .forEach(result::add);
        this.columnSchema = result;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ScriptReferenceDTO> getScripts() {
        return scripts;
    }

    public void setScripts(List<ScriptReferenceDTO> scripts) {
        this.scripts = scripts;
    }

    public Map<String,DataSetConstraintDTO> getConstraints() {
        return constraints;
    }

    public void setConstraints(Map<String,DataSetConstraintDTO> constraints) {
        this.constraints = constraints;
    }

    public String getDatabaseSchemaName() {
        return databaseSchemaName;
    }

    public void setDatabaseSchemaName(String databaseSchemaName) {
        this.databaseSchemaName = databaseSchemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public SQLTemplateDTO getSqlTemplate() {
        return sqlTemplate;
    }

    public void setSqlTemplate(SQLTemplateDTO sqlTemplate) {
        this.sqlTemplate = sqlTemplate;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DataSetDTO that = (DataSetDTO) o;
        return Objects.equals(getKeyName(), that.getKeyName()) && Objects.equals(getNameSpaceKeyName(), that.getNameSpaceKeyName()) && Objects.equals(getDataSourceKeyName(), that.getDataSourceKeyName()) && Objects.equals(getDatabaseSchemaName(), that.getDatabaseSchemaName()) && Objects.equals(getTableName(), that.getTableName()) && Objects.equals(getSources(), that.getSources()) && Objects.equals(getColumnSchema(), that.getColumnSchema()) && Objects.equals(getProperties(), that.getProperties()) && Objects.equals(getScripts(), that.getScripts()) && Objects.equals(getDescription(), that.getDescription()) && Objects.equals(getConstraints(), that.getConstraints()) && Objects.equals(getSqlTemplate(), that.getSqlTemplate()) && Objects.equals(getPartitionStmt(), that.getPartitionStmt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKeyName(), getNameSpaceKeyName(), getDataSourceKeyName(), getDatabaseSchemaName(), getTableName(), getSources(), getColumnSchema(), getProperties(), getScripts(), getDescription(), getConstraints(), getSqlTemplate(), getPartitionStmt());
    }

    @Override
    public String toString() {
        return "DataSetDTO{" +
                "keyName='" + keyName + '\'' +
                ", nameSpaceKeyName='" + nameSpaceKeyName + '\'' +
                ", dataSourceKeyName='" + dataSourceKeyName + '\'' +
                ", databaseSchemaName='" + databaseSchemaName + '\'' +
                ", tableName='" + tableName + '\'' +
                ", sources=" + sources +
                ", columnSchema=" + columnSchema +
                ", properties=" + properties +
                ", scripts=" + scripts +
                ", description='" + description + '\'' +
                ", constraints=" + constraints +
                ", sqlTemplate=" + sqlTemplate +
                ", partitionStmt='" + partitionStmt + '\'' +
                '}';
    }

    public String getPartitionStmt() {
        return partitionStmt;
    }

    public void setPartitionStmt(String partitionStmt) {
        this.partitionStmt = partitionStmt;
    }
}