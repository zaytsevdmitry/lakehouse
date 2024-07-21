package lakehouse.api.dto;

import java.io.Serializable;

public abstract class NameDescriptionAbstract implements Serializable {
    private String name;
    private String description;

    public NameDescriptionAbstract() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
