package org.lakehouse.client.api.dto.task;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.lakehouse.client.api.constant.SystemVarKeys;
import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.dataset.DataSetDTO;
import org.lakehouse.client.api.dto.configs.dataset.DataSetScriptDTO;
import org.lakehouse.client.api.dto.configs.datasource.DataSourceDTO;
import org.lakehouse.client.api.dto.configs.datasource.DriverDTO;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TaskProcessorConfigDTO {
    private String taskProcessorBody;
    private Map<String, String> taskProcessorArgs = new HashMap<>();
    private Map<String,String> scripts = new HashMap<>();
    private String targetDataSetKeyName;
    private Map<String, DriverDTO> drivers = new HashMap<>();
    private Map<String, DataSourceDTO> dataSources = new HashMap<>();
    private Map<String,DataSetDTO> dataSets = new HashMap<>();
    private OffsetDateTime targetDateTime;
    private OffsetDateTime intervalStartDateTime;
    private OffsetDateTime intervalEndDateTime;
    private String lockSource;
    private String taskFullName;

    public TaskProcessorConfigDTO() {
    }

    public void setTaskProcessorArgs(Map<String, String> taskProcessorArgs) {
        this.taskProcessorArgs = taskProcessorArgs;
    }

    public void setScripts(Map<String,String> scripts) {
        this.scripts = scripts;
    }


    public void setTargetDataSetKeyName(String targetDataSetKeyName) {
        this.targetDataSetKeyName = targetDataSetKeyName;
    }

    public void setDataSources(Map<String, DataSourceDTO> dataSources) {
        this.dataSources = dataSources;
    }

    public Map<String, String> getTaskProcessorArgs() {
        return taskProcessorArgs;
    }

    public Map<String,String> getScripts() {
        return scripts;
    }

    @JsonIgnore
    public String getTargetFullScript(){
        return getTargetDataSet()
                .getScripts()
                .stream()
                .sorted(Comparator.comparing(DataSetScriptDTO::getOrder))
                .map(script -> getScripts().get(script.getKey()))
                .collect(Collectors.joining(SystemVarKeys.SCRIPT_DELIMITER));

    };
    @JsonIgnore
    public DataSetDTO getTargetDataSet() {
        return getDataSets().get(getTargetDataSetKeyName());
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
                .entrySet()
                .stream()
                .filter(e -> e.getValue().getType().equals(Types.Constraint.foreign))
                .map(e -> getDataSets().get( e.getValue().getReference().getDataSetKeyName()))
                .collect(Collectors.toMap(DataSetDTO::getKeyName, Function.identity()));
    }

    public String getTargetDataSetKeyName() {
        return targetDataSetKeyName;
    }

    public Map<String, DataSourceDTO> getDataSources() {
        return dataSources;
    }

    public Map<String,DataSetDTO> getDataSets() {
        return dataSets;
    }

    public void setDataSets(Map<String,DataSetDTO> dataSets) {
        this.dataSets = dataSets;
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

    public Map<String, DriverDTO> getDrivers() {
        return drivers;
    }

    public void setDrivers(Map<String, DriverDTO> drivers) {
        this.drivers = drivers;
    }

    public String getTaskProcessorBody() {
        return taskProcessorBody;
    }

    public void setTaskProcessorBody(String taskProcessorBody) {
        this.taskProcessorBody = taskProcessorBody;
    }

    public String getTaskFullName() {
        return taskFullName;
    }

    public void setTaskFullName(String taskFullName) {
        this.taskFullName = taskFullName;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TaskProcessorConfigDTO that = (TaskProcessorConfigDTO) o;
        return Objects.equals(getTaskProcessorBody(), that.getTaskProcessorBody()) && Objects.equals(getTaskProcessorArgs(), that.getTaskProcessorArgs()) && Objects.equals(getScripts(), that.getScripts()) && Objects.equals(getTargetDataSetKeyName(), that.getTargetDataSetKeyName()) && Objects.equals(getDrivers(), that.getDrivers()) && Objects.equals(getDataSources(), that.getDataSources()) && Objects.equals(getDataSets(), that.getDataSets()) && Objects.equals(getTargetDateTime(), that.getTargetDateTime()) && Objects.equals(getIntervalStartDateTime(), that.getIntervalStartDateTime()) && Objects.equals(getIntervalEndDateTime(), that.getIntervalEndDateTime()) && Objects.equals(getLockSource(), that.getLockSource()) && Objects.equals(getTaskFullName(), that.getTaskFullName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTaskProcessorBody(), getTaskProcessorArgs(), getScripts(), getTargetDataSetKeyName(), getDrivers(), getDataSources(), getDataSets(), getTargetDateTime(), getIntervalStartDateTime(), getIntervalEndDateTime(), getLockSource(), getTaskFullName());
    }

}
