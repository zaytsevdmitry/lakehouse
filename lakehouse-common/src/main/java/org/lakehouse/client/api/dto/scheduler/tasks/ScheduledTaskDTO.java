package org.lakehouse.client.api.dto.scheduler.tasks;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.lakehouse.client.api.constant.Status;
import org.lakehouse.client.api.dto.configs.schedule.TaskDTO;

public class ScheduledTaskDTO extends TaskDTO {
    private Long id;
    private String scenarioActKeyName;
    private String scheduleKeyName;
    private Status.Task status;
    private String targetDateTime;
    private String intervalStartDateTime;
    private String intervalEndDateTime;
    private String dataSetKeyName;
    private Integer tryNum;
    public ScheduledTaskDTO() {
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getScenarioActKeyName() {
        return scenarioActKeyName;
    }

    public void setScenarioActKeyName(String scenarioActKeyName) {
        this.scenarioActKeyName = scenarioActKeyName;
    }

    public String getScheduleKeyName() {
        return scheduleKeyName;
    }

    public void setScheduleKeyName(String scheduleKeyName) {
        this.scheduleKeyName = scheduleKeyName;
    }

    public String getIntervalStartDateTime() {
        return intervalStartDateTime;
    }

    public String getTargetDateTime() {
        return targetDateTime;
    }

    public void setTargetDateTime(String targetDateTime) {
        this.targetDateTime = targetDateTime;
    }

    public void setIntervalStartDateTime(String intervalStartDateTime) {
        this.intervalStartDateTime = intervalStartDateTime;
    }

    public String getIntervalEndDateTime() {
        return intervalEndDateTime;
    }

    public void setIntervalEndDateTime(String intervalEndDateTime) {
        this.intervalEndDateTime = intervalEndDateTime;
    }

    public Status.Task getStatus() {
        return status;
    }

    public void setStatus(Status.Task status) {
        this.status = status;
    }

    public String getDataSetKeyName() {
        return dataSetKeyName;
    }

    public void setDataSetKeyName(String dataSetKeyName) {
        this.dataSetKeyName = dataSetKeyName;
    }

    public Integer getTryNum() {
        return tryNum;
    }

    public void setTryNum(Integer tryNum) {
        this.tryNum = tryNum;
    }

    @JsonIgnore
    public String getTaskFullName(){
        return String.format("%s.%s.%s.%s",
                getScheduleKeyName(),
                getScenarioActKeyName(),
                getName(),
                getTargetDateTime());
    }
    @JsonIgnore
    public String getLockSource(){
        return String.format("%s.%s.%s",
                getScheduleKeyName(),
                getScenarioActKeyName(),
                getTargetDateTime());
    }
}
