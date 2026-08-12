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

package org.lakehouse.client.api.dto.scheduler;

import org.lakehouse.client.api.constant.Status;

import java.util.Objects;

public class ScheduleTaskInstanceDTO {
    private Long id;

    private String name;

    private String beginDateTime;

    private String endDateTime;

    private Status.Task status;

    private int reTryNum;

    private String serviceId;

    private String causes;

    public ScheduleTaskInstanceDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBeginDateTime() {
        return beginDateTime;
    }

    public void setBeginDateTime(String beginDateTime) {
        this.beginDateTime = beginDateTime;
    }

    public String getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(String endDateTime) {
        this.endDateTime = endDateTime;
    }

    public Status.Task getStatus() {
        return status;
    }

    public void setStatus(Status.Task status) {
        this.status = status;
    }

    public int getReTryNum() {
        return reTryNum;
    }

    public void setReTryNum(int reTryNum) {
        this.reTryNum = reTryNum;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getCauses() {
        return causes;
    }

    public void setCauses(String causes) {
        this.causes = causes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ScheduleTaskInstanceDTO that = (ScheduleTaskInstanceDTO) o;
        return getReTryNum() == that.getReTryNum()
                && Objects.equals(getId(), that.getId())
                && Objects.equals(getName(), that.getName())
                && Objects.equals(getBeginDateTime(), that.getBeginDateTime())
                && Objects.equals(getEndDateTime(), that.getEndDateTime())
                && Objects.equals(getStatus(), that.getStatus())
                && Objects.equals(getServiceId(), that.getServiceId())
                && Objects.equals(getCauses(), that.getCauses());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getName(), getBeginDateTime(), getEndDateTime(), getStatus(), getReTryNum(),
                getServiceId(), getCauses());
    }

    @Override
    public String toString() {
        return "ScheduleTaskInstanceDTO{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", beginDateTime='" + beginDateTime + '\'' +
                ", endDateTime='" + endDateTime + '\'' +
                ", status=" + status +
                ", reTryNum=" + reTryNum +
                ", serviceId='" + serviceId + '\'' +
                ", causes='" + causes + '\'' +
                '}';
    }
}
