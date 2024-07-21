package lakehouse.api.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.sql.Timestamp;
@Entity
public class Schedule extends KeyEntityAbstract {

    @ManyToOne
    @JoinColumn(name = "data_set_key", referencedColumnName = "key")
    private DataSet dataSet;

    private String intervalExpression;
    private Timestamp startDateTime;

    @ManyToOne
    @JoinColumn(name = "scenario_template_key", referencedColumnName = "key")
    private ScenarioTemplate scenarioTemplate;

    @ManyToOne
    @JoinColumn(name = "task_execution_service_group_key", referencedColumnName = "key")
    private TaskExecutionServiceGroup taskExecutionServiceGroup;

    public Schedule(){}


    public Schedule(String key, String comment, DataSet dataSet, String intervalExpression, Timestamp startDateTime, ScenarioTemplate scenarioTemplate) {
        super(key, comment);
        this.dataSet = dataSet;
        this.intervalExpression = intervalExpression;
        this.startDateTime = startDateTime;
        this.scenarioTemplate = scenarioTemplate;
    }

    public DataSet getDataSet() {
        return dataSet;
    }

    public void setDataSet(DataSet dataSet) {
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

    public ScenarioTemplate getScenarioTemplate() {
        return scenarioTemplate;
    }

    public void setScenarioTemplate(ScenarioTemplate scenarioTemplate) {
        this.scenarioTemplate = scenarioTemplate;
    }
}
