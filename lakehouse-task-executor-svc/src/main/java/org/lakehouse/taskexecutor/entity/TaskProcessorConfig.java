package org.lakehouse.taskexecutor.entity;

import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TaskProcessorConfig {
    private  Map<String, String> executionModuleArgs;
    private  List<String> scripts;
    private  Map<String, DataSetDTO> sources;
    private  DataSetDTO targetDataSet;
    private  Map<String, DataStoreDTO> dataStores;
    private  Map<String, String> KeyBind;
    private  Map<String, TableDefinition> tableDefinitions;
    private Set<DataSetDTO> dataSetDTOSet;

    public TaskProcessorConfig(){}

    public void setExecutionModuleArgs(Map<String, String> executionModuleArgs) {
        this.executionModuleArgs = executionModuleArgs;
    }

    public void setScripts(List<String> scripts) {
        this.scripts = scripts;
    }

    public void setSources(Map<String, DataSetDTO> sources) {
        this.sources = sources;
    }

    public void setTargetDataSet(DataSetDTO targetDataSet) {
        this.targetDataSet = targetDataSet;
    }

    public void setDataStores(Map<String, DataStoreDTO> dataStores) {
        this.dataStores = dataStores;
    }

    public Map<String, String> getExecutionModuleArgs() {
        return executionModuleArgs;
    }

    public List<String> getScripts() {
        return scripts;
    }

    public Map<String, DataSetDTO> getSources() {
        return sources;
    }

    public DataSetDTO getTargetDataSet() {
        return targetDataSet;
    }

    public Map<String, DataStoreDTO> getDataStores() {
        return dataStores;
    }

    public Map<String, String> getKeyBind() {
        return KeyBind;
    }

    public void setKeyBind(Map<String, String> keyBind) {
        KeyBind = keyBind;
    }

    public Map<String, TableDefinition> getTableDefinitions() {
        return tableDefinitions;
    }

    public void setTableDefinitions(Map<String, TableDefinition> tableDefinitions) {
        this.tableDefinitions = tableDefinitions;
    }

    public Set<DataSetDTO> getDataSetDTOSet() {
        return dataSetDTOSet;
    }

    public void setDataSetDTOSet(Set<DataSetDTO> dataSetDTOSet) {
        this.dataSetDTOSet = dataSetDTOSet;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TaskProcessorConfig that = (TaskProcessorConfig) o;
        return Objects.equals(getExecutionModuleArgs(), that.getExecutionModuleArgs()) && Objects.equals(getScripts(), that.getScripts()) && Objects.equals(getSources(), that.getSources()) && Objects.equals(getTargetDataSet(), that.getTargetDataSet()) && Objects.equals(getDataStores(), that.getDataStores()) && Objects.equals(getKeyBind(), that.getKeyBind()) && Objects.equals(getTableDefinitions(), that.getTableDefinitions()) && Objects.equals(getDataSetDTOSet(), that.getDataSetDTOSet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getExecutionModuleArgs(), getScripts(), getSources(), getTargetDataSet(), getDataStores(), getKeyBind(), getTableDefinitions(), getDataSetDTOSet());
    }
}
