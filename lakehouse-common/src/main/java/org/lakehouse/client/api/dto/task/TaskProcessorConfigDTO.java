package org.lakehouse.client.api.dto.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TaskProcessorConfigDTO {
    private Map<String, String> executionModuleArgs = new HashMap<>();
    private List<String> scripts = new ArrayList<>();
    private String targetDataSetKeyName;
    private Map<String, DataSourceDTO> dataSources = new HashMap<>();
    private Map<String, String> KeyBind = new HashMap<>();
    private Map<String,DataSetDTO> dataSetDTOs = new HashMap<>();
    private OffsetDateTime targetDateTime;
    private OffsetDateTime intervalStartDateTime;
    private OffsetDateTime intervalEndDateTime;
    private String lockSource;


    public TaskProcessorConfigDTO() {
    }

    public void setExecutionModuleArgs(Map<String, String> executionModuleArgs) {
        this.executionModuleArgs = executionModuleArgs;
    }

    public void setScripts(List<String> scripts) {
        this.scripts = scripts;
    }


    public void setTargetDataSetKeyName(String targetDataSetKeyName) {
        this.targetDataSetKeyName = targetDataSetKeyName;
    }

    public void setDataSources(Map<String, DataSourceDTO> dataSources) {
        this.dataSources = dataSources;
    }

    public Map<String, String> getExecutionModuleArgs() {
        return executionModuleArgs;
    }

    public List<String> getScripts() {
        return scripts;
    }

    @JsonIgnore
    public DataSetDTO getTargetDataSet() {
        return getDataSetDTOs().get(getTargetDataSetKeyName());
    }

    @JsonIgnore
    public DataSourceDTO getTargetDataSourceDTO() {
        return
                getDataSources().get(getTargetDataSet().getDataSourceKeyName());
    }
    @JsonIgnore
    public Map<String, DataSetDTO> getForeignDataSetDTOMap(){
        return getTargetDataSet()
                .getConstraints()
                .stream()
                .filter(dataSetConstraintDTO -> dataSetConstraintDTO.getType().equals(Types.Constraint.foreign))
                .map(dataSetConstraintDTO -> getDataSetDTOs().get( dataSetConstraintDTO.getReference().getDataSetKeyName()))
                .collect(Collectors.toMap(DataSetDTO::getKeyName, Function.identity()));
    }

    public String getTargetDataSetKeyName() {
        return targetDataSetKeyName;
    }

    public Map<String, DataSourceDTO> getDataSources() {
        return dataSources;
    }

    public Map<String, String> getKeyBind() {
        return KeyBind;
    }

    public void setKeyBind(Map<String, String> keyBind) {
        KeyBind = keyBind;
    }

    public Map<String,DataSetDTO> getDataSetDTOs() {
        return dataSetDTOs;
    }

    public void setDataSetDTOs(Map<String,DataSetDTO> dataSetDTOs) {
        this.dataSetDTOs = dataSetDTOs;
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

    public String getLockSource() {
        return lockSource;
    }

    public void setLockSource(String lockSource) {
        this.lockSource = lockSource;
    }

    public OffsetDateTime getTargetDateTime() {
        return targetDateTime;
    }

    public void setTargetDateTime(OffsetDateTime targetDateTime) {
        this.targetDateTime = targetDateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TaskProcessorConfigDTO that = (TaskProcessorConfigDTO) o;
        return Objects.equals(executionModuleArgs, that.executionModuleArgs)
                && Objects.equals(scripts, that.scripts)
                && Objects.equals(targetDataSetKeyName, that.targetDataSetKeyName)
                && Objects.equals(dataSources, that.dataSources)
                && Objects.equals(KeyBind, that.KeyBind)
                && Objects.equals(dataSetDTOs, that.dataSetDTOs)
                && Objects.equals(targetDateTime, that.targetDateTime)
                && Objects.equals(intervalStartDateTime, that.intervalStartDateTime)
                && Objects.equals(intervalEndDateTime, that.intervalEndDateTime)
                && Objects.equals(lockSource, that.lockSource);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                executionModuleArgs,
                scripts,
                targetDataSetKeyName,
                dataSources,
                KeyBind,
                dataSetDTOs,
                targetDateTime,
                intervalStartDateTime,
                intervalEndDateTime,
                lockSource);
    }
}
