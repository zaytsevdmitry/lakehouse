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

import java.io.Serial;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ScheduleDTO extends ScheduleAbstract {
    @Serial
    private static final long serialVersionUID = 5259060075468520559L;

    private Set<ScheduleScenarioActDTO> scenarioActs = new HashSet<>();

    public Set<ScheduleScenarioActDTO> getScenarioActs() {
        return scenarioActs;
    }

    public void setScenarioActs(Set<ScheduleScenarioActDTO> scenarioActs) {
        this.scenarioActs = scenarioActs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ScheduleDTO that = (ScheduleDTO) o;
        return super.equals(o)
                && Objects.equals(getScenarioActs(), that.getScenarioActs());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getScenarioActs());
    }

    @Override
    public ScheduleDTO copy() throws Exception {
        ScheduleDTO result = (ScheduleDTO) super.copy();
        result.setScenarioActs(getScenarioActs());
        if (result.equals(this))
            return result;
        else throw new Exception(String.format("Error when copy of %s", this.getClass().getName()));
    }

}
