package org.lakehouse.taskexecutor.entity;

import org.lakehouse.client.api.dto.configs.DataSetDTO;
import org.lakehouse.client.api.dto.configs.DataStoreDTO;

import java.util.List;
import java.util.Map;

public class TaskProcessorConfig {
    private  Map<String, String> executionModuleArgs;
    private  List<String> scripts;
    private  Map<String, DataSetDTO> sources;
    private  DataSetDTO targetDataSet;
    private  Map<String, DataStoreDTO> dataStores;
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
}
