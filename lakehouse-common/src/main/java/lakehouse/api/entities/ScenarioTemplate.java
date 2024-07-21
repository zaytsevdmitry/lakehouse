package lakehouse.api.entities;

import jakarta.persistence.Entity;

@Entity
public class ScenarioTemplate extends KeyEntityAbstract {

    public ScenarioTemplate() {}

    public ScenarioTemplate(String key, String comment) {
        super(key, comment);
    }
}
