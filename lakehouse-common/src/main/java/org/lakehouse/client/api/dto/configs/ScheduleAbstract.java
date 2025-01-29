package org.lakehouse.client.api.dto.configs;

import org.lakehouse.client.api.utils.DateTimeUtils;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ScheduleAbstract extends NameDescriptionAbstract
{
    @Serial
    private static final long serialVersionUID = 5872306801909970542L;
	private String intervalExpression;
    private String startDateTime;
    private String stopDateTime;
    private List<DagEdgeDTO> scenarioActEdges = new ArrayList<>();
    private boolean enabled;
    public ScheduleAbstract() {}

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

    public List<DagEdgeDTO> getScenarioActEdges() {
        return scenarioActEdges;
    }

    public void setScenarioActEdges(List<DagEdgeDTO> scenarioActEdges) {
        this.scenarioActEdges = scenarioActEdges
                .stream() // sort for stable list comparison
                .sorted(Comparator.comparing(DagEdgeDTO::hashCode))
                .toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ScheduleAbstract that = (ScheduleAbstract) o;
        return isEnabled() == that.isEnabled()
                && Objects.equals(getIntervalExpression(), that.getIntervalExpression())
                && Objects.equals(//todo resolve what is this
                        DateTimeUtils.parceDateTimeFormatWithTZ(getStartDateTime()),
                        DateTimeUtils.parceDateTimeFormatWithTZ(that.getStartDateTime()))
                && Objects.equals(getStopDateTime(), that.getStopDateTime())

                && Objects.equals(getScenarioActEdges(), that.getScenarioActEdges());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getIntervalExpression(), getStartDateTime(), getStopDateTime(),  getScenarioActEdges(), isEnabled());
    }
    
    public ScheduleAbstract copy() throws Exception {
        ScheduleAbstract result = new ScheduleAbstract();
        result.setEnabled(isEnabled());
        result.setName(getName());
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
