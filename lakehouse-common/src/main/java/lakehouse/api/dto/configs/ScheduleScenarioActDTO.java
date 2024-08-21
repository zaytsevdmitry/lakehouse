package lakehouse.api.dto.configs;

import java.util.Objects;

public class ScheduleScenarioActDTO {
    private String name;
    private String dataSet;
    private String scenarioActTemplate;

    public ScheduleScenarioActDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataSet() {
        return dataSet;
    }

    public void setDataSet(String dataSet) {
        this.dataSet = dataSet;
    }

    public String getScenarioActTemplate() {
        return scenarioActTemplate;
    }

    public void setScenarioActTemplate(String scenarioActTemplate) {
        this.scenarioActTemplate = scenarioActTemplate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScheduleScenarioActDTO that = (ScheduleScenarioActDTO) o;
        return Objects.equals(getName(), that.getName()) && Objects.equals(getDataSet(), that.getDataSet()) && Objects.equals(getScenarioActTemplate(), that.getScenarioActTemplate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getDataSet(), getScenarioActTemplate());
    }
}
