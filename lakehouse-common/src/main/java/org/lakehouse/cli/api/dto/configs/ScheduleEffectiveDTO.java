package org.lakehouse.cli.api.dto.configs;

import org.lakehouse.cli.api.utils.DateTimeUtils;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ScheduleEffectiveDTO extends ScheduleAbstract
{
    @Serial
    private static final long serialVersionUID = 2955161378713705887L;

    private List<ScheduleScenarioActEffectiveDTO> scenarioActs = new ArrayList<>();

    public ScheduleEffectiveDTO() {}


    public List<ScheduleScenarioActEffectiveDTO> getScenarioActs() {
        return scenarioActs;
    }

    public void setScenarioActs(List<ScheduleScenarioActEffectiveDTO> scenarioActs) {
        this.scenarioActs = scenarioActs
                .stream() // sort for stable list comparison
                .sorted(Comparator.comparing(ScheduleScenarioActEffectiveDTO::hashCode))
                .toList();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ScheduleEffectiveDTO that = (ScheduleEffectiveDTO) o;
        return isEnabled() == that.isEnabled()
                && Objects.equals(getIntervalExpression(), that.getIntervalExpression())
                && Objects.equals(//todo resolve what is this
                        DateTimeUtils.parceDateTimeFormatWithTZ(getStartDateTime()),
                        DateTimeUtils.parceDateTimeFormatWithTZ(that.getStartDateTime()))
                && Objects.equals(getStopDateTime(), that.getStopDateTime())
                && Objects.equals(getScenarioActs(), that.getScenarioActs())
                && Objects.equals(getScenarioActEdges(), that.getScenarioActEdges());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getIntervalExpression(), getStartDateTime(), getStopDateTime(), getScenarioActs(), getScenarioActEdges(), isEnabled());
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
