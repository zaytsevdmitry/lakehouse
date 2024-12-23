package org.lakehouse.cli.api.dto.configs;

import org.lakehouse.cli.api.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ScheduleDTO extends ScheduleAbstract
{
    private static final long serialVersionUID = 5259060075468520559L;

    private List<ScheduleScenarioActDTO> scenarioActs = new ArrayList<>();

    public List<ScheduleScenarioActDTO> getScenarioActs() {
        return scenarioActs;
    }

    public void setScenarioActs(List<ScheduleScenarioActDTO> scenarioActs) {
        this.scenarioActs = scenarioActs
                .stream() // sort for stable list comparison
                .sorted(Comparator.comparing(ScheduleScenarioActDTO::hashCode))
                .toList();
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
        return Objects.hash(super.hashCode(),  getScenarioActs());
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
