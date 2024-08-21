package lakehouse.api.dto.configs;

import lakehouse.api.utils.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ScheduleDTO extends NameDescriptionAbstract
{
    private String intervalExpression;
    private String startDateTime;
    private String stopDateTime;
    private List<ScheduleScenarioActDTO> scenarioActs = new ArrayList<>();
    private List<DagEdgeDTO> scenarioActEdges = new ArrayList<>();
    private boolean enabled;
    public ScheduleDTO() {}

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

    public List<ScheduleScenarioActDTO> getScenarioActs() {
        return scenarioActs;
    }

    public void setScenarioActs(List<ScheduleScenarioActDTO> scenarioActs) {
        this.scenarioActs = scenarioActs;
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
        this.scenarioActEdges = scenarioActEdges;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ScheduleDTO that = (ScheduleDTO) o;
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
}
