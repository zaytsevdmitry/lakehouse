package lakehouse.api.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class DataSetDTO implements Serializable {
    private String key;
    private String project;
    private String dataStore;
    private List<String> sources;
    private List<ColumnDTO> columnSchema;
    private Map<String,String> properties;
    private String comment;
    public DataSetDTO() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public List<String> getSources() {
        return sources;
    }

    public void setSources(List<String> sources) {
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

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
