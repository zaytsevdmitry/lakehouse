package org.lakehouse.client.api.dto.scheduler.tasks;

import org.lakehouse.client.api.dto.configs.TaskDTO;

public class ScheduledTaskDTO extends TaskDTO {
    private Long id;
    private String scenarioActKeyName;
    private String scheduleKeyName;
    private String status;
    private String targetDateTime;
    private String intervalStartDateTime;
    private String intervalEndDateTime;
    private String dataSetKeyName;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDataSetKeyName() {
        return dataSetKeyName;
    }

    public void setDataSetKeyName(String dataSetKeyName) {
        this.dataSetKeyName = dataSetKeyName;
    }
}
