package org.lakehouse.client.api.dto.configs;

import java.util.Objects;

public class ScriptReferenceDTO {
    private String key;
    private Integer order;

    public ScriptReferenceDTO() {
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ScriptReferenceDTO that = (ScriptReferenceDTO) o;
        return Objects.equals(getKey(), that.getKey()) && Objects.equals(getOrder(), that.getOrder());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getOrder());
    }

    @Override
    public String toString() {
        return "\nScriptReferenceDTO{" +
                "\nkey='" + key + '\'' +
                "\n, order=" + order +
                '}';
    }
}
