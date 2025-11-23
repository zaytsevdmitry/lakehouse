package org.lakehouse.client.api.dto.configs.dataset;

import java.io.Serializable;
import java.util.*;

public class DataSetDTO implements Serializable {
    private static final long serialVersionUID = -7115041705179190105L;
    private String keyName;
    private String nameSpaceKeyName;
    private String dataSourceKeyName;
    private String fullTableName;
    private List<DataSetSourceDTO> sources = new ArrayList<>();
    private List<ColumnDTO> columnSchema = new ArrayList<>();
    private Map<String, String> properties = new HashMap<>();
    private List<DataSetScriptDTO> scripts = new ArrayList<>();
    private String description;
    private List<DataSetConstraintDTO> constraints = new ArrayList<>();

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

    public List<DataSetSourceDTO> getSources() {
        return sources;
    }

    public void setSources(List<DataSetSourceDTO> sources) {
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

    public List<DataSetScriptDTO> getScripts() {
        return scripts;
    }

    public void setScripts(List<DataSetScriptDTO> scripts) {
        this.scripts = scripts;
    }

    public List<DataSetConstraintDTO> getConstraints() {
        return constraints;
    }

    public void setConstraints(List<DataSetConstraintDTO> constraints) {
        this.constraints = constraints;
    }

    public String getFullTableName() {
        return fullTableName;
    }

    public void setFullTableName(String fullTableName) {
        this.fullTableName = fullTableName;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DataSetDTO that = (DataSetDTO) o;
        return Objects.equals(getKeyName(), that.getKeyName())
                && Objects.equals(getNameSpaceKeyName(), that.getNameSpaceKeyName())
                && Objects.equals(getDataSourceKeyName(), that.getDataSourceKeyName())
                && Objects.equals(getFullTableName(), that.getFullTableName())
                && Objects.equals(getSources(), that.getSources())
                && Objects.equals(getColumnSchema(), that.getColumnSchema())
                && Objects.equals(getProperties(), that.getProperties())
                && Objects.equals(getScripts(), that.getScripts())
                && Objects.equals(getDescription(), that.getDescription())
                && Objects.equals(getConstraints(), that.getConstraints());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKeyName(), getNameSpaceKeyName(), getDataSourceKeyName(), getFullTableName(), getSources(), getColumnSchema(),
                getProperties(), getScripts(), getDescription(), getConstraints());
    }
}