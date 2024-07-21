package lakehouse.api.entities;

import jakarta.persistence.Entity;

@Entity

public class Project extends KeyEntityAbstract {

    public Project() {
    }

    public Project(String key, String name, String comment) {
        super(key, comment);
    }
}
