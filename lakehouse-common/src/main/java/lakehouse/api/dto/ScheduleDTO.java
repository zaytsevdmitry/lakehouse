package lakehouse.api.dto;

import java.sql.Timestamp;
import java.util.Objects;

public class ScheduleDTO extends NameDescriptionAbstract
{
    private String dataSet;
    private String intervalExpression;
    private Timestamp startDateTime;
    private Timestamp endDateTime;
    private String scenarioTemplate;
    private boolean enabled;
    public ScheduleDTO() {}

    public String getDataSet() {
        return dataSet;
    }

    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }

    public String getIntervalExpression() {
        return intervalExpression;
    }

    public void setIntervalExpression(String intervalExpression) {
        this.intervalExpression = intervalExpression;
    }

    public Timestamp getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(Timestamp startDateTime) {
        this.startDateTime = startDateTime;
    }

    public Timestamp getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(Timestamp endDateTime) {
        this.endDateTime = endDateTime;
    }

    public String getScenarioTemplate() {
        return scenarioTemplate;
    }

    public void setScenarioTemplate(String scenarioTemplate) {
        this.scenarioTemplate = scenarioTemplate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ScheduleDTO that = (ScheduleDTO) o;
        return isEnabled() == that.isEnabled() && Objects.equals(getDataSet(), that.getDataSet()) && Objects.equals(getIntervalExpression(), that.getIntervalExpression()) && Objects.equals(getStartDateTime(), that.getStartDateTime()) && Objects.equals(getEndDateTime(), that.getEndDateTime()) && Objects.equals(getScenarioTemplate(), that.getScenarioTemplate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDataSet(), getIntervalExpression(), getStartDateTime(), getEndDateTime(), getScenarioTemplate(), isEnabled());
    }
}
