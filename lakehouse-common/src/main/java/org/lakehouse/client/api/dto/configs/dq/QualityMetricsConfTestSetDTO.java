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

package org.lakehouse.client.api.dto.configs.dq;

import org.lakehouse.client.api.constant.Types;
import org.lakehouse.client.api.dto.configs.ScriptReferenceDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QualityMetricsConfTestSetDTO {
    private String description;
    private Types.DQMetricTestSetType type;
    private List<ScriptReferenceDTO> scripts = new ArrayList<>();

    public QualityMetricsConfTestSetDTO() {
    }

    public Types.DQMetricTestSetType getType() {
        return type;
    }

    public void setType(Types.DQMetricTestSetType type) {
        this.type = type;
    }

    public List<ScriptReferenceDTO> getScripts() {
        return scripts;
    }

    public void setScripts(List<ScriptReferenceDTO> scripts) {
        this.scripts = scripts;
    }

    @Override
    public String toString() {
        return "QualityMetricsConfTestSetDTO{" +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", scripts=" + scripts +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        QualityMetricsConfTestSetDTO that = (QualityMetricsConfTestSetDTO) o;
        return Objects.equals(getDescription(), that.getDescription()) && getType() == that.getType() && Objects.equals(getScripts(), that.getScripts());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDescription(), getType(),  getScripts());
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
