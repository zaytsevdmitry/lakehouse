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
