/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 * 
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
    public String buildTaskFullName(){
        return (String.format("%d-%d-%s-%s-%s-%s",
                getId(),
                getTryNum(),
                getScheduleKeyName(),
                getScenarioActKeyName(),
                getName(),
                getTargetDateTime()
                        .replace("-","")
                        .replace(":","")));
    }
    @JsonIgnore
    public String buildLockSource(){
        return String.format("%s.%s.%s",
                getScheduleKeyName(),
                getScenarioActKeyName(),
                getTargetDateTime());
    }
}
