package lakehouse.api.entities.configs;

import jakarta.persistence.*;

import java.util.Objects;
@MappedSuperclass
public abstract class KeyValueAbstract{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String key;

    @Column(nullable = false)
    private String value;

    public KeyValueAbstract() {}

    public KeyValueAbstract(  String key, String value) {

        this.key = key;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return getKey() + " -> " + getValue();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof KeyValueAbstract keyValueAbstract))
            return false;
        return Objects.equals(getKey(), keyValueAbstract.getKey())
                && Objects.equals(getValue(), keyValueAbstract.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getValue());
    }
}
