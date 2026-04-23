package org.lakehouse.client.api.dto.configs;


import java.io.Serializable;
import java.util.Objects;

public class NameSpaceDTO implements Serializable {
    private static final long serialVersionUID = -3879303744430009909L;
    private String keyName;
    private String description;

    public NameSpaceDTO() {

    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
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
        NameSpaceDTO that = (NameSpaceDTO) o;
        return Objects.equals(keyName, that.keyName) && Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyName, description);
    }
}
