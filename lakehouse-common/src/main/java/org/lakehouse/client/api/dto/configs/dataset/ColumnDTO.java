package org.lakehouse.client.api.dto.configs.dataset;


import org.lakehouse.client.api.dto.configs.NameDescriptionAbstract;

import java.util.Objects;

public class ColumnDTO extends NameDescriptionAbstract {


    private String dataType;
    private boolean nullable;
    private Integer order = null;
    private boolean sequence;

    public ColumnDTO() {
    }


    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Integer getOrder() {
        return order;
    }

    public boolean isSequence() {
        return sequence;
    }

    public void setSequence(boolean sequence) {
        this.sequence = sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ColumnDTO columnDTO = (ColumnDTO) o;
        return isNullable() == columnDTO.isNullable() && isSequence() == columnDTO.isSequence() && Objects.equals(getDataType(), columnDTO.getDataType()) && Objects.equals(getOrder(), columnDTO.getOrder()) && Objects.equals(getDescription(), columnDTO.getDescription());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDataType(), isNullable(), getOrder(), isSequence(), getDescription());
    }

    @Override
    public String toString() {
        return "\nColumnDTO{" +
                "\ndataType='" + dataType + '\'' +
                "\n, nullable=" + nullable +
                "\n, order=" + order +
                "\n, sequence=" + sequence +
                '}';
    }
}
