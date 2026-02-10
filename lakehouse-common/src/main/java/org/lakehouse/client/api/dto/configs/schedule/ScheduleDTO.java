package org.lakehouse.client.api.dto.configs.schedule;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ScheduleDTO extends ScheduleAbstract {
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
