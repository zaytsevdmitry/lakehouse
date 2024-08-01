package lakehouse.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import java.sql.Timestamp;
import java.util.Objects;

@Entity
public class Schedule extends KeyEntityAbstract {

    @ManyToOne
    private DataSet dataSet;

    @ManyToOne
    private ScenarioTemplate scenarioTemplate;

    @ManyToOne
    private TaskExecutionServiceGroup taskExecutionServiceGroup;

    private String intervalExpression;
    private Timestamp startDateTime;
    private Timestamp endDateTime;
    private boolean enabled;

    public DataSet getDataSet() {
        return dataSet;
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    public ScenarioTemplate getScenarioTemplate() {
        return scenarioTemplate;
    }

    public void setScenarioTemplate(ScenarioTemplate scenarioTemplate) {
        this.scenarioTemplate = scenarioTemplate;
    }

    public TaskExecutionServiceGroup getTaskExecutionServiceGroup() {
        return taskExecutionServiceGroup;
    }

    public void setTaskExecutionServiceGroup(TaskExecutionServiceGroup taskExecutionServiceGroup) {
        this.taskExecutionServiceGroup = taskExecutionServiceGroup;
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
        Schedule schedule = (Schedule) o;
        return isEnabled() == schedule.isEnabled() && Objects.equals(getDataSet(), schedule.getDataSet()) && Objects.equals(getScenarioTemplate(), schedule.getScenarioTemplate()) && Objects.equals(getTaskExecutionServiceGroup(), schedule.getTaskExecutionServiceGroup()) && Objects.equals(getIntervalExpression(), schedule.getIntervalExpression()) && Objects.equals(getStartDateTime(), schedule.getStartDateTime()) && Objects.equals(getEndDateTime(), schedule.getEndDateTime());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getDataSet(), getScenarioTemplate(), getTaskExecutionServiceGroup(), getIntervalExpression(), getStartDateTime(), getEndDateTime(), isEnabled());
    }
}
