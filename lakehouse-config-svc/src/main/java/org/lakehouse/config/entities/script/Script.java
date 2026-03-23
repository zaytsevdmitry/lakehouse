package org.lakehouse.config.entities.script;

import jakarta.persistence.*;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "script_key_uk", columnNames = {"key"})
}
)
public class Script {
    @Id
    @Column(nullable = false)
    private String key;
    @Lob
    @Column(nullable = false)
    private String value;

    public Script() {
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


}
