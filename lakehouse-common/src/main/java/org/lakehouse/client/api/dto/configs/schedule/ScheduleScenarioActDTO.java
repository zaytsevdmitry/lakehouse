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


import java.util.Objects;

public class ScheduleScenarioActDTO extends ScheduleScenarioActAbstract {

    private String scenarioActTemplate;


    public ScheduleScenarioActDTO() {
    }

    public String getScenarioActTemplate() {
        return scenarioActTemplate;
    }

    public void setScenarioActTemplate(String scenarioActTemplate) {
        this.scenarioActTemplate = scenarioActTemplate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ScheduleScenarioActDTO that = (ScheduleScenarioActDTO) o;
        return Objects.equals(getScenarioActTemplate(), that.getScenarioActTemplate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getScenarioActTemplate());
    }
}
