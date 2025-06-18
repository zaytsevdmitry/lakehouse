package org.lakehouse.taskexecutor.entity;

import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;

import java.time.OffsetDateTime;
import java.util.*;

public class TaskProcessorConfig {
    private  Map<String, String> executionModuleArgs = new HashMap<>();
    private  List<String> scripts = new ArrayList<>();
    private  Map<String, DataSetDTO> sources = new HashMap<>();
    private  DataSetDTO targetDataSet;
    private  Map<String, DataStoreDTO> dataStores = new HashMap<>();
    private  Map<String, String> KeyBind = new HashMap<>();
    private  Map<String, TableDefinition> tableDefinitions = new HashMap<>();
    private  Set<DataSetDTO> dataSetDTOSet = new HashSet<>();
    private OffsetDateTime intervalStartDateTime;
    private OffsetDateTime intervalEndDateTime;

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

    public OffsetDateTime getIntervalStartDateTime() {
        return intervalStartDateTime;
    }

    public void setIntervalStartDateTime(OffsetDateTime intervalStartDateTime) {
        this.intervalStartDateTime = intervalStartDateTime;
    }

    public OffsetDateTime getIntervalEndDateTime() {
        return intervalEndDateTime;
    }

    public void setIntervalEndDateTime(OffsetDateTime intervalEndDateTime) {
        this.intervalEndDateTime = intervalEndDateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskProcessorConfig that = (TaskProcessorConfig) o;
        return Objects.equals(getExecutionModuleArgs(), that.getExecutionModuleArgs()) && Objects.equals(getScripts(), that.getScripts()) && Objects.equals(getSources(), that.getSources()) && Objects.equals(getTargetDataSet(), that.getTargetDataSet()) && Objects.equals(getDataStores(), that.getDataStores()) && Objects.equals(getKeyBind(), that.getKeyBind()) && Objects.equals(getTableDefinitions(), that.getTableDefinitions()) && Objects.equals(getDataSetDTOSet(), that.getDataSetDTOSet()) && Objects.equals(getIntervalStartDateTime(), that.getIntervalStartDateTime()) && Objects.equals(getIntervalEndDateTime(), that.getIntervalEndDateTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getExecutionModuleArgs(), getScripts(), getSources(), getTargetDataSet(), getDataStores(), getKeyBind(), getTableDefinitions(), getDataSetDTOSet(), getIntervalStartDateTime(), getIntervalEndDateTime());
    }
}
