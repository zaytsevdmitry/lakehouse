/*
 * "Lakehouse management tool" - the services set for managing data changes based on a metadata-driven approach
 * Copyright (C) 2026  Dmitry Zaytsev https://github.com/zaytsevdmitry/lakehouse
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0.txt
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
