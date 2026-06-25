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
