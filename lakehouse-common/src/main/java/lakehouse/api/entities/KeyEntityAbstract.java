package lakehouse.api.entities;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

import java.util.Objects;

@MappedSuperclass
public abstract class KeyEntityAbstract{
    @Id
    private String key;
    private String comment;

    public KeyEntityAbstract() {}

    public KeyEntityAbstract(String key, String comment) {
        this.key = key;
        this.comment = comment;

    }


    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return getKey();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof KeyEntityAbstract keyEntity))
            return false;
        return Objects.equals(getKey(), keyEntity.getKey())
                && Objects.equals(getComment(), keyEntity.getComment());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getComment());
    }
}
