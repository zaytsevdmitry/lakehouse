package lakehouse.api.dto.configs;

import java.io.Serializable;
import java.util.*;

public class DataSetDTO implements Serializable {
    private String name;
    private String project;
    private String dataStore;
    private List<DataSetSourceDTO> sources = new ArrayList<>();
    private List<ColumnDTO> columnSchema = new ArrayList<>();
    private Map<String,String> properties = new HashMap<>();
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


        this.columnSchema = columnSchema
                .stream()
                .sorted(Comparator.comparing(NameDescriptionAbstract::getName))
                .toList();
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
        boolean result;
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataSetDTO that = (DataSetDTO) o;
        result =  Objects.equals(getColumnSchema(), that.getColumnSchema());
        result = result  &&  Objects.equals(getName(), that.getName());

        result = result  && Objects.equals(getProject(), that.getProject());
        result = result && Objects.equals(getDataStore(), that.getDataStore());
        result = result  && Objects.equals(getSources(), that.getSources());

        result = result  && Objects.equals(getProperties(), that.getProperties());
        result = result  && Objects.equals(getDescription(), that.getDescription());

        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getProject(), getDataStore(), getSources(), getColumnSchema(), getProperties(), getDescription());
    }
}
