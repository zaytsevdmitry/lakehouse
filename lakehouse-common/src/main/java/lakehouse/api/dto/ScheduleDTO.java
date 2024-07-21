package lakehouse.api.dto;

import java.sql.Timestamp;

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
}
