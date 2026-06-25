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

package org.lakehouse.client.api.dto.configs.schedule;

import org.lakehouse.client.api.dto.configs.DagEdgeDTO;
import org.lakehouse.client.api.dto.configs.KeyNameDescriptionAbstract;
import org.lakehouse.client.api.utils.DateTimeUtils;

import java.io.Serial;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ScheduleAbstract extends KeyNameDescriptionAbstract {
    @Serial
    private static final long serialVersionUID = 5872306801909970542L;
    private String intervalExpression;
    private String startDateTime; // use DateTimeUtils.strEquals to compare
    private String stopDateTime;
    private Set<DagEdgeDTO> scenarioActEdges = new HashSet<>();
    private boolean enabled;

    public ScheduleAbstract() {
    }

    public String getIntervalExpression() {
        return intervalExpression;
    }

    public void setIntervalExpression(String intervalExpression) {
        this.intervalExpression = intervalExpression;
    }

    public String getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(String startDateTime) {
        this.startDateTime = startDateTime;
    }

    public String getStopDateTime() {
        return stopDateTime;
    }

    public void setStopDateTime(String stopDateTime) {
        this.stopDateTime = stopDateTime;
    }


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<DagEdgeDTO> getScenarioActEdges() {
        return scenarioActEdges;
    }

    public void setScenarioActEdges(Set<DagEdgeDTO> scenarioActEdges) {
        this.scenarioActEdges = scenarioActEdges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ScheduleAbstract that = (ScheduleAbstract) o;
        return isEnabled() == that.isEnabled()
                && Objects.equals(getIntervalExpression(), that.getIntervalExpression())
                && DateTimeUtils.strEquals(getStartDateTime(), that.getStartDateTime())
                && DateTimeUtils.strEquals(getStopDateTime(), that.getStopDateTime())
                && Objects.equals(getScenarioActEdges(), that.getScenarioActEdges());
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                super.hashCode(),
                getIntervalExpression(),
                DateTimeUtils.parseDateTimeFormatWithTZ(getStartDateTime()),
                DateTimeUtils.parseDateTimeFormatWithTZ(getStopDateTime()),
                getScenarioActEdges(),
                isEnabled());
    }

    public ScheduleAbstract copy() throws Exception {
        ScheduleAbstract result = new ScheduleAbstract();
        result.setEnabled(isEnabled());
        result.setKeyName(getKeyName());
        result.setIntervalExpression(getIntervalExpression());
        result.setStartDateTime(getStartDateTime());
        result.setStopDateTime(getStopDateTime());
        result.setScenarioActEdges(getScenarioActEdges());
        result.setDescription(getDescription());
        if (result.equals(this))
            return result;
        else throw new Exception(String.format("Error when copy of %s", this.getClass().getName()));
    }

}
