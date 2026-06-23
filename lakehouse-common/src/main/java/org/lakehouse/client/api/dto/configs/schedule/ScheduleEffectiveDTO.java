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

import org.lakehouse.client.api.utils.DateTimeUtils;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

public class ScheduleEffectiveDTO extends ScheduleAbstract {
    @Serial
    private static final long serialVersionUID = 2955161378713705887L;

    private Long lastChangeNumber;

    private String lastChangedDateTime;

    private Set<ScheduleScenarioActEffectiveDTO> scenarioActs = new HashSet<>();

    public ScheduleEffectiveDTO() {
    }


    public Set<ScheduleScenarioActEffectiveDTO> getScenarioActs() {
        return scenarioActs;
    }

    public void setScenarioActs(Set<ScheduleScenarioActEffectiveDTO> scenarioActs) {
        this.scenarioActs = scenarioActs.stream()
                .sorted(Comparator.comparing(ScheduleScenarioActAbstract::getName))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public void setLastChangeNumber(Long lastChangeNumber) {
        this.lastChangeNumber = lastChangeNumber;
    }


    public Long getLastChangeNumber() {
        return lastChangeNumber;
    }

    public String getLastChangedDateTime() {
        return lastChangedDateTime;
    }

    public void setLastChangedDateTime(String lastChangedDateTime) {
        this.lastChangedDateTime = lastChangedDateTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ScheduleEffectiveDTO that = (ScheduleEffectiveDTO) o;
        return Objects.equals(getLastChangeNumber(), that.getLastChangeNumber())
                && DateTimeUtils.strEquals(getLastChangedDateTime(), that.getLastChangedDateTime())
                && Objects.equals(getScenarioActs(), that.getScenarioActs());
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(),
                getLastChangeNumber(),
                DateTimeUtils.parseDateTimeFormatWithTZ(getLastChangedDateTime()),
                getScenarioActs());
    }

    @Override
    public ScheduleEffectiveDTO copy() throws Exception {
        ScheduleEffectiveDTO result = (ScheduleEffectiveDTO) super.copy();
        result.setScenarioActs(getScenarioActs());
        if (result.equals(this))
            return result;
        else throw new Exception(String.format("Error when copy of %s", this.getClass().getName()));
    }

}
