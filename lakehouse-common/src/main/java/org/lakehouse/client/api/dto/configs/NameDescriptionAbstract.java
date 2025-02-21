package org.lakehouse.client.api.dto.configs;

import java.io.Serializable;
import java.util.Objects;

public abstract class NameDescriptionAbstract implements Serializable {
    private static final long serialVersionUID = 6749318353497672650L;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NameDescriptionAbstract that = (NameDescriptionAbstract) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }
}
