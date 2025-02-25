package org.lakehouse.client.api.dto.configs;

import java.util.Objects;

public class DataSetScriptDTO {
    private String key;
    private Integer order;
    public DataSetScriptDTO() {
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
        DataSetScriptDTO that = (DataSetScriptDTO) o;
        return Objects.equals(getKey(), that.getKey()) && Objects.equals(getOrder(), that.getOrder());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), getOrder());
    }
}
