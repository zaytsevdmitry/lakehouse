package lakehouse.api.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DataSetDTO implements Serializable {
    private String name;
    private String project;
    private String dataStore;
    private List<DataSetSourceDTO> sources;
    private List<ColumnDTO> columnSchema;
    private Map<String,String> properties;
    private String description;
    public DataSetDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getDataStore() {
        return dataStore;
    }

    public void setDataStore(String dataStore) {
        this.dataStore = dataStore;
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
        this.columnSchema = columnSchema;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSetDTO that = (DataSetDTO) o;
        return Objects.equals(getName(), that.getName()) && Objects.equals(getProject(), that.getProject()) && Objects.equals(getDataStore(), that.getDataStore()) && Objects.equals(getSources(), that.getSources()) && Objects.equals(getColumnSchema(), that.getColumnSchema()) && Objects.equals(getProperties(), that.getProperties()) && Objects.equals(getDescription(), that.getDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProject(), getDataStore(), getSources(), getColumnSchema(), getProperties(), getDescription());
    }
}
