package org.lakehouse.client.api.dto.configs;


import java.util.Objects;

public class ScheduleScenarioActDTO extends ScheduleScenarioActAbstract {

    private String scenarioActTemplate;


    public ScheduleScenarioActDTO() {
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
        if (!super.equals(o)) return false;
        ScheduleScenarioActDTO that = (ScheduleScenarioActDTO) o;
        return Objects.equals(getScenarioActTemplate(), that.getScenarioActTemplate());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getScenarioActTemplate());
    }
}
